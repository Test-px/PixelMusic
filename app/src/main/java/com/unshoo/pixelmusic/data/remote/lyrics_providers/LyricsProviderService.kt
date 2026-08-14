package com.unshoo.pixelmusic.data.remote.lyrics_providers

import com.unshoo.pixelmusic.data.remote.lyrics_providers.model.SongInfo
import com.unshoo.pixelmusic.data.remote.lyrics_providers.others.LRCLibAPI
import com.unshoo.pixelmusic.data.remote.lyrics_providers.util.Exceptions.*
import com.unshoo.pixelmusic.data.remote.lyrics_providers.util.Providers
import okhttp3.OkHttpClient

class LyricsProviderService(client: OkHttpClient) {
    
    private val lrcLibAPI = LRCLibAPI(client)
    // We will initialize AppleAPI, SpotifyAPI, NeteaseAPI here in Phase 3!

    suspend fun getSongInfo(query: SongInfo, offset: Int = 0, provider: Providers): SongInfo? {
        return try {
            when (provider) {
                Providers.LRCLIB -> lrcLibAPI.getSongInfo(query, offset) ?: throw Exception("No track found")
                else -> throw Exception("Provider not implemented yet")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getSyncedLyrics(song: SongInfo, provider: Providers): String? {
        return when (provider) {
            Providers.LRCLIB -> lrcLibAPI.getSyncedLyrics(song.lrcLibID ?: 0)
            else -> null
        }
    }
}

