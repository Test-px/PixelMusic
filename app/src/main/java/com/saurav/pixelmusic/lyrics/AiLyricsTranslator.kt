package com.saurav.pixelmusic.lyrics

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.saurav.pixelmusic.data.ai.provider.AiClientFactory
import com.saurav.pixelmusic.data.ai.provider.AiProvider
import com.saurav.pixelmusic.data.preferences.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray

/**
 * Translates synced lyric lines using the user's configured AI provider and API key
 * (from AI Settings). Returns null on missing key, error, or mismatched line count so
 * callers can fall back cleanly to [GoogleTranslateEngine].
 */
object AiLyricsTranslator {

    private const val ChunkSize = 50
    private val AiProviderKey = stringPreferencesKey("ai_provider")

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val clientFactory = AiClientFactory()

    private val cacheLock = Any()
    private val cache = object : LinkedHashMap<String, List<String>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>?): Boolean = size > 64
    }

    suspend fun translate(
        context: Context,
        lines: List<String>,
        targetLanguageName: String,
    ): List<String>? = withContext(Dispatchers.IO) {
        if (lines.isEmpty() || targetLanguageName.isBlank()) return@withContext null

        val prefs = runCatching { context.dataStore.data.first() }.getOrNull() ?: return@withContext null
        val providerName = prefs[AiProviderKey] ?: AiProvider.GEMINI.name
        val provider = AiProvider.fromString(providerName)
        val apiKey = prefs[stringPreferencesKey("${provider.name.lowercase()}_api_key")].orEmpty().trim()
        if (apiKey.isBlank()) return@withContext null

        val savedModel = prefs[stringPreferencesKey("${provider.name.lowercase()}_model")].orEmpty().trim()
        val client = runCatching { clientFactory.createClient(provider, apiKey) }.getOrNull() ?: return@withContext null
        val model = savedModel.ifBlank { client.getDefaultModel() }

        val cacheKey = "${provider.name}\u0000$model\u0000$targetLanguageName\u0000${lines.joinToString("\u0001")}"
        synchronized(cacheLock) { cache[cacheKey] }?.let { return@withContext it }

        val systemPrompt = """
            You are a song lyrics translator. Translate each lyric line naturally into $targetLanguageName while preserving the emotional tone and meaning.
            Return ONLY a valid JSON array of translated strings with the EXACT same number of elements and order as the input array.
            Do not wrap the response in markdown code fences or add any explanation.
        """.trimIndent()

        val out = ArrayList<String>(lines.size)
        for (chunk in lines.chunked(ChunkSize)) {
            val inputJson = JsonArray(chunk.map { JsonPrimitive(it) }).toString()
            val prompt = "Translate these ${chunk.size} lyric lines into $targetLanguageName:\n$inputJson"

            val raw = runCatching {
                client.generateContent(
                    model = model,
                    systemPrompt = systemPrompt,
                    prompt = prompt,
                    temperature = 0.3f,
                )
            }.getOrNull() ?: return@withContext null

            val parsed = parseJsonStringArray(raw, expectedSize = chunk.size) ?: return@withContext null
            out += parsed
        }

        if (out.size != lines.size) return@withContext null
        synchronized(cacheLock) { cache[cacheKey] = out }
        out
    }

    private fun parseJsonStringArray(raw: String, expectedSize: Int): List<String>? = runCatching {
        val cleaned = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        if (start < 0 || end <= start) return@runCatching null

        val array = json.parseToJsonElement(cleaned.substring(start, end + 1)).jsonArray
        if (array.size != expectedSize) return@runCatching null
        array.map { item ->
            (item as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim() ?: return@runCatching null
        }
    }.getOrNull()
}
