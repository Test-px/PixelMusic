package com.saurav.pixelmusic.lyrics

import android.content.Context
import com.saurav.pixelmusic.data.model.Lyrics
import com.saurav.pixelmusic.lyrics.providers.betterlyrics.BetterLyrics
import com.saurav.pixelmusic.lyrics.providers.betterlyrics.TTMLParser
import com.saurav.pixelmusic.lyrics.providers.directlyrics.Deezer
import com.saurav.pixelmusic.lyrics.providers.directlyrics.LrcRed
import com.saurav.pixelmusic.lyrics.providers.directlyrics.NetEase
import com.saurav.pixelmusic.lyrics.providers.directlyrics.QQMusic
import com.saurav.pixelmusic.lyrics.providers.kugou.KuGou
import com.saurav.pixelmusic.lyrics.providers.lrclib.LrcLib
import com.saurav.pixelmusic.lyrics.providers.paxsenix.Paxsenix
import com.saurav.pixelmusic.lyrics.providers.simpmusic.SimpMusicLyrics
import com.saurav.pixelmusic.lyrics.providers.youlyplus.YouLyPlus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

data class MochyLyricsResult(
    val lyrics: Lyrics,
    val rawLrc: String,
    val providerId: String,
    val providerDisplayName: String,
)

/**
 * Multi-provider lyrics engine ported from Mochy: queries BetterLyrics (Apple Music TTML),
 * YouLyPlus, Paxsenix, lrc.red, SimpMusic, LrcLib, Deezer, NetEase, QQ Music, and KuGou
 * in the user's configured priority order.
 */
object MochyLyricsEngine {

    fun getDisplayName(providerId: String): String = when (providerId) {
        "BetterLyrics" -> "Apple Music"
        "YouLyPlus" -> "YouLyPlus"
        "Paxsenix" -> "PaxSenix"
        "LrcRed" -> "lrc.red"
        "SimpMusic" -> "SimpMusic"
        "LrcLib" -> "LRCLIB"
        "Deezer" -> "Deezer"
        "NetEase" -> "NetEase"
        "QQMusic" -> "QQ Music"
        "Kugou" -> "KuGou"
        else -> providerId
    }

    suspend fun fetchBestLyrics(
        context: Context,
        title: String,
        artist: String,
        durationSec: Int,
        album: String? = null,
        youtubeId: String? = null,
    ): MochyLyricsResult? = withContext(Dispatchers.IO) {
        val order = runCatching { MochyLyricsPreferences.providerOrderFlow(context).first() }
            .getOrDefault(MochyLyricsPreferences.DefaultProviderOrder)

        var plainFallback: MochyLyricsResult? = null
        var lineSyncedFallback: MochyLyricsResult? = null

        for (providerId in order) {
            val res = fetchFromProvider(
                context = context,
                providerId = providerId,
                title = title,
                artist = artist,
                durationSec = durationSec,
                album = album,
                youtubeId = youtubeId,
            ) ?: continue

            val hasWordSync = res.lyrics.synced?.any { !it.words.isNullOrEmpty() } == true
            val hasLineSync = !res.lyrics.synced.isNullOrEmpty()

            if (hasWordSync) {
                return@withContext res
            }
            if (hasLineSync && lineSyncedFallback == null) {
                lineSyncedFallback = res
                // If we already got a line-synced result from a top-tier provider, return right away
                if (providerId in TopTierFastReturn) {
                    return@withContext res
                }
            }
            if (plainFallback == null && !res.lyrics.plain.isNullOrEmpty()) {
                plainFallback = res
            }
        }

        lineSyncedFallback ?: plainFallback
    }

    suspend fun fetchFromProvider(
        context: Context,
        providerId: String,
        title: String,
        artist: String,
        durationSec: Int,
        album: String? = null,
        youtubeId: String? = null,
    ): MochyLyricsResult? = withContext(Dispatchers.IO) {
        val raw = withTimeoutOrNull(9_000L) {
            runCatching {
                when (providerId) {
                    "BetterLyrics" -> BetterLyrics.getLyrics(title, artist, durationSec, album).getOrNull()
                    "YouLyPlus" -> YouLyPlus.getLyrics(title, artist, durationSec, album, id = youtubeId).getOrNull()
                    "Paxsenix" -> {
                        Paxsenix.init(context)
                        Paxsenix.getLyrics(title, artist, durationSec, album).getOrNull()
                    }
                    "LrcRed" -> LrcRed.getTtml(title, artist, durationSec).mapCatching { ttml ->
                        val lines = TTMLParser.parseTTML(ttml)
                        check(lines.isNotEmpty())
                        TTMLParser.toLRC(lines)
                    }.getOrNull()
                    "SimpMusic" -> {
                        val vid = youtubeId?.takeIf { it.length == 11 } ?: return@runCatching null
                        SimpMusicLyrics.getLyrics(vid, durationSec).getOrNull()
                    }
                    "LrcLib" -> LrcLib.getLyrics(title, artist, durationSec, album).getOrNull()
                    "Deezer" -> Deezer.getLyrics(title, artist, durationSec).getOrNull()
                    "NetEase" -> NetEase.getLyrics(title, artist, durationSec).getOrNull()
                    "QQMusic" -> QQMusic.getLyrics(title, artist, durationSec).getOrNull()
                    "Kugou" -> KuGou.getLyrics(title, artist, durationSec, album).getOrNull()
                    else -> null
                }
            }.onFailure { e ->
                Timber.tag("MochyLyrics").d(e, "Provider %s missed for %s", providerId, title)
            }.getOrNull()
        }?.takeIf { it.isNotBlank() } ?: return@withContext null

        val displayName = getDisplayName(providerId)
        val parsed = MochyLyricsParser.parse(raw, providerName = displayName, areFromRemote = true)
        if (parsed.synced.isNullOrEmpty() && parsed.plain.isNullOrEmpty()) return@withContext null

        MochyLyricsResult(
            lyrics = parsed,
            rawLrc = raw,
            providerId = providerId,
            providerDisplayName = displayName,
        )
    }

    suspend fun searchAllProviders(
        context: Context,
        title: String,
        artist: String,
        durationSec: Int,
        album: String? = null,
        youtubeId: String? = null,
    ): List<MochyLyricsResult> = coroutineScope {
        val order = runCatching { MochyLyricsPreferences.providerOrderFlow(context).first() }
            .getOrDefault(MochyLyricsPreferences.DefaultProviderOrder)

        order.map { providerId ->
            async(Dispatchers.IO) {
                fetchFromProvider(context, providerId, title, artist, durationSec, album, youtubeId)
            }
        }.awaitAll().filterNotNull()
    }

    private val TopTierFastReturn = setOf("BetterLyrics", "YouLyPlus", "Paxsenix", "LrcRed", "SimpMusic", "LrcLib")
}
