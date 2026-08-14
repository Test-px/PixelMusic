package com.unshoo.pixelmusic.data.remote.lyrics_providers

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import com.unshoo.pixelmusic.data.remote.lyrics_providers.others.LRCLibAPI
import com.unshoo.pixelmusic.data.remote.lyrics_providers.util.Providers
import com.unshoo.pixelmusic.data.remote.lyrics_providers.util.matching.*

private const val TAG = "SmartLyricsMatcher"

data class ScoredHit(
    val provider: Providers,
    val strategy: MatchStrategy,
    val result: ProviderResult,
    val confidence: ConfidenceBreakdown,
    val inlineLyrics: String?,   
    val neteaseId: Long?,        
) {
    val tier get() = confidence.tier
}

data class MatchConfig(
    val providerOrder: List<Providers> = listOf(Providers.LRCLIB), // We will add NETEASE, SPOTIFY here later
    val maxRetries: Int = 3,
    val requestDelayMs: Long = 200,
    val maxCandidatesPerProvider: Int = 4,
    val retryBaseDelayMs: Long = 500,
    val providerTimeoutMs: Long = 25_000,
)

class SmartLyricsMatcher(
    private val lrcLib: LRCLibAPI,
    // private val netease: NeteaseAPI, (Will add in Phase 3)
    // private val providerService: LyricsProviderService? = null, (Will add in Phase 3)
) {
    suspend fun search(
        local: LocalTrack,
        candidates: List<QueryCandidate>,
        config: MatchConfig = MatchConfig(),
        log: (String) -> Unit = { Log.i(TAG, it) },
    ): List<ScoredHit> = coroutineScope {
        val displayName = listOfNotNull(local.artist, local.title).joinToString(" - ").ifBlank { "<unknown>" }
        log("[match] \"$displayName\"  dur=${local.durationSec?.let { "${it.toInt()}s" } ?: "?"}  candidates=${candidates.size}  providers=${config.providerOrder.size}")

        val jobs = config.providerOrder.map { provider ->
            async {
                withTimeoutOrNull(config.providerTimeoutMs) {
                    searchProvider(provider, local, candidates, config, log)
                } ?: emptyList<ScoredHit>().also { log("  [${provider.name}] skipped/timeout") }
            }
        }

        val hits = LinkedHashMap<String, ScoredHit>() 
        var best = 0.0
        var stopped = false
        for ((index, deferred) in jobs.withIndex()) {
            if (stopped && !deferred.isCompleted) {
                deferred.cancel()
                continue
            }
            for (hit in deferred.await()) {
                val key = "${hit.provider}|${hit.result.title}|${hit.result.artist}|${hit.result.durationSec}"
                val existing = hits[key]
                if (existing == null || hit.confidence.score > existing.confidence.score) hits[key] = hit
                if (hit.tier != MatchTier.REJECT && hit.confidence.score > best) {
                    best = hit.confidence.score
                    log("  [${hit.provider.name}] ${hit.strategy.label} -> ${hit.result.title} (${hit.confidence.percent()}%)")
                }
            }
            if (!stopped && best >= ConfidenceScorer.AUTO_ACCEPT_THRESHOLD) stopped = true
        }

        hits.values.sortedByDescending { it.confidence.score }
    }

    private suspend fun searchProvider(
        provider: Providers,
        local: LocalTrack,
        candidates: List<QueryCandidate>,
        config: MatchConfig,
        log: (String) -> Unit,
    ): List<ScoredHit> {
        val providerHits = LinkedHashMap<String, ScoredHit>()
        var providerBest = 0.0
        val artistGuesses = artistGuessesFor(local, candidates)
        
        for (cand in candidates) {
            val query = cand.asSearchString()
            if (query.isBlank()) continue

            val found = when (provider) {
                Providers.LRCLIB -> searchLrcLib(query, local, cand, config, log)
                // Providers.NETEASE -> searchNetease(...)
                else -> emptyList() // Will implement Generic search (Spotify/Apple) later
            }
            
            for (raw in found) {
                val hit = if (wrongSingerVetoed(artistGuesses, raw.result.artist, raw.confidence))
                    raw.copy(confidence = raw.confidence.copy(tier = MatchTier.REJECT))
                else raw
                
                val key = "${hit.provider}|${hit.result.title}|${hit.result.artist}|${hit.result.durationSec}"
                val existing = providerHits[key]
                if (existing == null || hit.confidence.score > existing.confidence.score) providerHits[key] = hit
                if (hit.tier != MatchTier.REJECT && hit.confidence.score > providerBest) providerBest = hit.confidence.score
            }
            if (providerBest >= ConfidenceScorer.AUTO_ACCEPT_THRESHOLD) break
            delay(config.requestDelayMs)
        }
        return providerHits.values.toList()
    }

    private suspend fun searchLrcLib(
        query: String, local: LocalTrack, cand: QueryCandidate, config: MatchConfig, log: (String) -> Unit
    ): List<ScoredHit> {
        val results = runCatching {
            // Using a simple retry loop instead of Ktor's withRetry
            var res: List<com.unshoo.pixelmusic.data.network.lyrics.LrcLibResponse>? = null
            for (i in 0 until config.maxRetries) {
                try {
                    res = lrcLib.searchCandidates(query)
                    if (res.isNotEmpty()) break
                } catch (e: Exception) { delay(config.retryBaseDelayMs * (i + 1)) }
            }
            res ?: emptyList()
        }.getOrDefault(emptyList())

        return results
            .filter { !it.syncedLyrics.isNullOrBlank() } 
            .map { r ->
                val pr = ProviderResult(r.trackName, r.artistName, r.duration, r.albumName, true)
                ScoredHit(Providers.LRCLIB, cand.strategy, pr, scoreHitAgainstViews(local, pr, cand), r.syncedLyrics, null)
            }
            .sortedByDescending { it.confidence.score }
            .take(config.maxCandidatesPerProvider)
    }

    suspend fun fetchLyrics(hit: ScoredHit, config: MatchConfig = MatchConfig(), log: (String) -> Unit = {}): String? {
        return hit.inlineLyrics // Will add Netease secondary fetch here later
    }
}

internal fun scoreHitAgainstViews(local: LocalTrack, pr: ProviderResult, cand: QueryCandidate): ConfidenceBreakdown {
    var best = ConfidenceScorer.score(local, pr)
    val view = LocalTrack(cand.title, cand.artist, local.durationSec, local.album)
    if (view.title != local.title || view.artist != local.artist) {
        val viewConf = ConfidenceScorer.score(view, pr)
        val viewTrustable = !cand.artist.isNullOrBlank() || viewConf.durationMatched
        if (viewTrustable && viewConf.score > best.score) best = viewConf
    }
    return if (cand.strategy == MatchStrategy.FILENAME_LOOSE && best.tier == MatchTier.AUTO_ACCEPT)
        best.copy(score = 0.80, tier = MatchTier.REVIEW)
    else best
}

internal fun artistGuessesFor(local: LocalTrack, candidates: List<QueryCandidate>): List<String> =
    (listOfNotNull(local.artist) + candidates.mapNotNull { it.artist }).filter { it.isNotBlank() }.distinct()

internal fun wrongSingerVetoed(artistGuesses: List<String>, resultArtist: String?, conf: ConfidenceBreakdown): Boolean {
    if (conf.durationMatched) return false
    if (resultArtist.isNullOrBlank()) return false
    val guesses = artistGuesses.filter { TextMatch.normalizeForCompare(it).isNotEmpty() }
    if (guesses.isEmpty()) return false
    if (TextMatch.normalizeForCompare(resultArtist).isEmpty()) return true
    return guesses.maxOf { TextMatch.similarity(it, resultArtist) } < 0.50
}

