package com.saurav.pixelmusic.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray

object GoogleTranslateEngine {

    private const val LineMarker = "\n@@@\n"
    private const val BatchCharBudget = 1_500
    private const val LineByLineParallelism = 4

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val romanizeCacheLock = Any()
    private val romanizeCache = object : LinkedHashMap<String, List<String?>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String?>>?): Boolean = size > 64
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
        }
    }

    suspend fun translate(
        lines: List<String>,
        targetLanguage: String,
        sourceLanguage: String = "auto",
    ): List<String>? = withContext(Dispatchers.IO) {
        if (lines.isEmpty() || targetLanguage.isBlank()) return@withContext null

        val out = ArrayList<String>(lines.size)
        var batch = ArrayList<String>()
        var budget = 0

        suspend fun flush(): Boolean {
            if (batch.isEmpty()) return true
            val translated = translateBatch(batch, targetLanguage, sourceLanguage)
                ?: translateLineByLine(batch, targetLanguage, sourceLanguage)
                ?: return false
            out += translated
            batch = ArrayList()
            budget = 0
            return true
        }

        for (line in lines) {
            if (budget + line.length > BatchCharBudget && batch.isNotEmpty()) {
                if (!flush()) return@withContext null
            }
            batch.add(line)
            budget += line.length + LineMarker.length
        }
        if (!flush()) return@withContext null

        if (out.size == lines.size) out else null
    }

    private suspend fun translateLineByLine(
        lines: List<String>,
        targetLanguage: String,
        sourceLanguage: String,
    ): List<String>? = coroutineScope {
        val gate = Semaphore(LineByLineParallelism)
        lines.map { line ->
            async { gate.withPermit { translateBatch(listOf(line), targetLanguage, sourceLanguage)?.single() } }
        }.awaitAll().let { results ->
            results.mapIndexed { index, translated -> translated ?: lines[index] }
                .takeIf { results.any { it != null } }
        }
    }

    private suspend fun translateBatch(
        lines: List<String>,
        targetLanguage: String,
        sourceLanguage: String,
    ): List<String>? {
        val payload = lines.joinToString(LineMarker)
        val body = runCatching {
            val response = client.get("https://translate.googleapis.com/translate_a/single") {
                parameter("client", "gtx")
                parameter("sl", sourceLanguage)
                parameter("tl", targetLanguage)
                parameter("dt", "t")
                parameter("q", payload)
            }
            if (response.status.value != 200) return null
            response.bodyAsText()
        }.getOrNull() ?: return null

        val joined = runCatching {
            val root = json.parseToJsonElement(body).jsonArray
            val segments = root.getOrNull(0) as? JsonArray ?: return null
            buildString {
                for (segment in segments) {
                    val piece = (segment as? JsonArray)?.getOrNull(0) as? JsonPrimitive ?: continue
                    append(piece.content)
                }
            }
        }.getOrNull() ?: return null

        val parts = joined.split(Regex("\\s*@@@\\s*")).map { it.trim() }
        return if (parts.size == lines.size) parts else null
    }

    suspend fun romanize(
        lines: List<String>,
        sourceLanguage: String = "auto",
    ): List<String?>? = withContext(Dispatchers.IO) {
        if (lines.isEmpty()) return@withContext null

        val cacheKey = "$sourceLanguage\u0000${lines.joinToString("\u0001")}"
        synchronized(romanizeCacheLock) {
            romanizeCache[cacheKey]
        }?.let { return@withContext it }

        val out = ArrayList<String?>(lines.size)
        var batch = ArrayList<String>()
        var budget = 0

        suspend fun flush(): Boolean {
            if (batch.isEmpty()) return true
            val romanized = romanizeBatch(batch, sourceLanguage)
                ?: romanizeLineByLine(batch, sourceLanguage)
                ?: return false
            out += romanized
            batch = ArrayList()
            budget = 0
            return true
        }

        for (line in lines) {
            if (budget + line.length > BatchCharBudget && batch.isNotEmpty()) {
                if (!flush()) return@withContext null
            }
            batch.add(line)
            budget += line.length + LineMarker.length
        }
        if (!flush()) return@withContext null

        if (out.size != lines.size) return@withContext null
        val cleaned = out.mapIndexed { idx, rom ->
            val trimmed = rom?.trim()
            if (trimmed.isNullOrEmpty() || trimmed.equals(lines[idx].trim(), ignoreCase = true)) null else trimmed
        }
        if (cleaned.any { it != null }) {
            synchronized(romanizeCacheLock) {
                romanizeCache[cacheKey] = cleaned
            }
        }
        cleaned
    }

    private suspend fun romanizeLineByLine(
        lines: List<String>,
        sourceLanguage: String,
    ): List<String?>? = coroutineScope {
        val gate = Semaphore(LineByLineParallelism)
        val results = lines.map { line ->
            async { gate.withPermit { romanizeBatch(listOf(line), sourceLanguage)?.singleOrNull() } }
        }.awaitAll()
        results.takeIf { it.any { rom -> !rom.isNullOrBlank() } }
    }

    private suspend fun romanizeBatch(
        lines: List<String>,
        sourceLanguage: String,
    ): List<String?>? {
        val payload = lines.joinToString(LineMarker)
        val body = runCatching {
            val response = client.get("https://translate.googleapis.com/translate_a/single") {
                parameter("client", "gtx")
                parameter("sl", sourceLanguage)
                parameter("tl", "en")
                parameter("dt", "rm")
                parameter("q", payload)
            }
            if (response.status.value != 200) return null
            response.bodyAsText()
        }.getOrNull() ?: return null

        val joined = runCatching {
            val root = json.parseToJsonElement(body).jsonArray
            val segments = root.getOrNull(0) as? JsonArray ?: return null
            buildString {
                for (segment in segments) {
                    val piece = (segment as? JsonArray)?.getOrNull(3) as? JsonPrimitive
                    if (piece != null && piece.isString) {
                        append(piece.content)
                    }
                }
            }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null

        val parts = joined.split(Regex("\\s*[@＠]{3}\\s*")).map { it.trim() }
        return if (parts.size == lines.size) parts else null
    }
}
