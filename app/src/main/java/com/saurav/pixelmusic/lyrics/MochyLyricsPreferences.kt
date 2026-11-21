package com.saurav.pixelmusic.lyrics

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.saurav.pixelmusic.data.preferences.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class MochyLyricsStyle(
    val displayName: String,
    val description: String,
    val risesWords: Boolean = false,
    val shaded: Boolean = false,
) {
    MOTION(
        displayName = "Style 1",
        description = "Words rise as they're sung, long notes swell, lines cascade",
        risesWords = true,
        shaded = true,
    ),
    LEGACY(
        displayName = "Style 2",
        description = "The original wave across the sung letters",
        risesWords = false,
        shaded = true,
    ),
    BORING(
        displayName = "Boring",
        description = "Style 1's rise, without the glow",
        risesWords = true,
        shaded = false,
    ),
    PLAIN(
        displayName = "Plain",
        description = "Style 2's sweep, without the glow or the wave",
        risesWords = false,
        shaded = false,
    ),
    PIXELMUSIC(
        displayName = "PixelMusic",
        description = "Original PixelMusic spring-animated synced lyrics",
        risesWords = false,
        shaded = false,
    );

    companion object {
        fun fromName(name: String?): MochyLyricsStyle = when (name?.uppercase()) {
            "MOTION", "APPLE", "APPLE_V2", "MOCHY_1", "GLOW" -> MOTION
            "LEGACY", "KARAOKE", "METRO_LYRICS" -> LEGACY
            "BORING", "SLIDE" -> BORING
            "PLAIN", "LYRICS_V2", "FADE", "NONE" -> PLAIN
            "PIXELMUSIC" -> PIXELMUSIC
            else -> MOTION
        }
    }
}

object MochyLyricsPreferences {
    val StyleKey = stringPreferencesKey("mochy_lyrics_style")
    val TextSizeKey = floatPreferencesKey("mochy_lyrics_text_size")
    val LineSpacingKey = floatPreferencesKey("mochy_lyrics_line_spacing")
    val SoftLightKey = booleanPreferencesKey("mochy_lyrics_soft_light")
    val AutoTranslateKey = booleanPreferencesKey("mochy_lyrics_auto_translate")
    val TranslateLanguageKey = stringPreferencesKey("mochy_lyrics_translate_lang")
    val AutoRomanizeKey = booleanPreferencesKey("mochy_lyrics_auto_romanize")
    val RomanizationAsMainKey = booleanPreferencesKey("mochy_lyrics_romanization_main")
    val ProviderOrderKey = stringPreferencesKey("mochy_lyrics_provider_order")

    val DefaultProviderOrder: List<String> = listOf(
        "BetterLyrics",
        "YouLyPlus",
        "Paxsenix",
        "LrcRed",
        "SimpMusic",
        "LrcLib",
        "Deezer",
        "NetEase",
        "QQMusic",
        "Kugou",
    )

    val SupportedTranslateLanguages: List<Pair<String, String>> = listOf(
        "en" to "English",
        "hi" to "Hindi",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "ja" to "Japanese",
        "ko" to "Korean",
        "zh-CN" to "Chinese",
        "ar" to "Arabic",
        "pt" to "Portuguese",
        "ru" to "Russian",
        "id" to "Indonesian",
        "tr" to "Turkish",
        "bn" to "Bengali",
    )

    fun styleFlow(context: Context): Flow<MochyLyricsStyle> =
        context.dataStore.data.map { MochyLyricsStyle.fromName(it[StyleKey]) }

    fun textSizeFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { (it[TextSizeKey] ?: 26f).coerceIn(18f, 36f) }

    fun lineSpacingFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { (it[LineSpacingKey] ?: 1.25f).coerceIn(1.0f, 1.7f) }

    fun softLightFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[SoftLightKey] ?: true }

    fun autoTranslateFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[AutoTranslateKey] ?: false }

    fun translateLanguageFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[TranslateLanguageKey]?.ifBlank { "en" } ?: "en" }

    fun autoRomanizeFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[AutoRomanizeKey] ?: false }

    fun romanizationAsMainFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[RomanizationAsMainKey] ?: false }

    fun providerOrderFlow(context: Context): Flow<List<String>> =
        context.dataStore.data.map { deserializeProviderOrder(it[ProviderOrderKey].orEmpty()) }

    fun deserializeProviderOrder(raw: String): List<String> {
        if (raw.isBlank()) return DefaultProviderOrder
        val saved = raw.split(",").map { it.trim() }.filter { it in DefaultProviderOrder }.distinct()
        return saved + DefaultProviderOrder.filter { it !in saved }
    }

    suspend fun setStyle(context: Context, style: MochyLyricsStyle) {
        context.dataStore.edit { it[StyleKey] = style.name }
    }

    suspend fun setTextSize(context: Context, sizeSp: Float) {
        context.dataStore.edit { it[TextSizeKey] = sizeSp.coerceIn(18f, 36f) }
    }

    suspend fun setLineSpacing(context: Context, spacing: Float) {
        context.dataStore.edit { it[LineSpacingKey] = spacing.coerceIn(1.0f, 1.7f) }
    }

    suspend fun setSoftLight(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[SoftLightKey] = enabled }
    }

    suspend fun setAutoTranslate(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[AutoTranslateKey] = enabled }
    }

    suspend fun setTranslateLanguage(context: Context, langCode: String) {
        context.dataStore.edit { it[TranslateLanguageKey] = langCode }
    }

    suspend fun setAutoRomanize(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[AutoRomanizeKey] = enabled }
    }

    suspend fun setRomanizationAsMain(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[RomanizationAsMainKey] = enabled }
    }

    suspend fun setProviderOrder(context: Context, order: List<String>) {
        val clean = order.filter { it in DefaultProviderOrder }.distinct()
        val full = clean + DefaultProviderOrder.filter { it !in clean }
        context.dataStore.edit { it[ProviderOrderKey] = full.joinToString(",") }
    }
}
