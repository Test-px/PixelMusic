package com.unshoo.pixelmusic.data.remote.lyrics_providers

import com.unshoo.pixelmusic.data.remote.lyrics_providers.model.SongInfo
import com.unshoo.pixelmusic.data.remote.lyrics_providers.others.LRCLibAPI
import com.unshoo.pixelmusic.data.remote.lyrics_providers.others.NeteaseAPI
import com.unshoo.pixelmusic.data.remote.lyrics_providers.spotify.SpotifyAPI
import com.unshoo.pixelmusic.data.remote.lyrics_providers.util.Exceptions.*
import com.unshoo.pixelmusic.data.remote.lyrics_providers.util.Providers
import okhttp3.OkHttpClient

class LyricsProviderService(client: OkHttpClient) {
    
    private val lrcLibAPI = LRCLibAPI(client)
    private val neteaseAPI = NeteaseAPI(client)
    private val spotifyAPI = SpotifyAPI(client)

    suspend fun getSongInfo(query: SongInfo, offset: Int = 0, provider: Providers): SongInfo? {
        return try {
            when (provider) {
                Providers.LRCLIB -> lrcLibAPI.getSongInfo(query, offset) ?: throw NoTrackFoundException()
                Providers.NETEASE -> neteaseAPI.getSongInfo(query, offset) ?: throw NoTrackFoundException()
                Providers.SPOTIFY -> spotifyAPI.getSongInfo(query, offset) ?: throw NoTrackFoundException()
                else -> throw InternalErrorException("Provider not implemented yet")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getSyncedLyrics(song: SongInfo, provider: Providers): String? {
        return when (provider) {
            Providers.LRCLIB -> lrcLibAPI.getSyncedLyrics(song.lrcLibID ?: 0)
            Providers.NETEASE -> neteaseAPI.getSyncedLyrics(song.neteaseID ?: 0L)
            Providers.SPOTIFY -> spotifyAPI.getSyncedLyrics(song.songLink ?: "")
            else -> null
        }
    }
}
