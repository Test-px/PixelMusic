package com.unshoo.pixelmusic.data.remote.youtube

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.unshoo.pixelmusic.R
import com.unshoo.pixelmusic.data.database.youtube.AppDatabase
import com.unshoo.pixelmusic.data.model.youtube.Song
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.absoluteValue

class SongDownloadWorker(
    private val appContext: Context,
    private val params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun musicDao(): com.unshoo.pixelmusic.data.database.MusicDao
    }

    private val playlistRepository = AppDatabase.getInstance(appContext).playlistRepository()
    private val localSongRepository = AppDatabase.getInstance(appContext).songRepository()
    private val songRepository = SongRepository()
    private val musicDao = EntryPointAccessors.fromApplication(
        appContext,
        WorkerEntryPoint::class.java
    ).musicDao()

    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "song_download_channel"

    @OptIn(UnstableApi::class)
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val playlistId = params.inputData.getString(PLAYLIST_KEY)
            val songId = params.inputData.getString(SONG_KEY) ?: return@withContext Result.failure()
            val persistPublicly = params.inputData.getBoolean("persist_publicly", true)
            val notificationId = songId.hashCode().absoluteValue

            var song = localSongRepository.getSong(songId)
            if (song == null) {
                var fetchedSong: Song? = null
                songRepository.getSongInfo(songId).collect { apiResult ->
                    if (apiResult is ApiResult.Success) {
                        fetchedSong = apiResult.data
                    }
                }
                song = fetchedSong ?: return@withContext Result.failure()
                localSongRepository.create(song)
            }

            if (playlistId != null) {
                val playlist = playlistRepository.getPlaylistById(playlistId)
                if (playlist != null) {
                    val playlistImage = DownloadHelper.downloadImage(appContext, playlist.info.coverHref, playlist.info.id)
                    playlistRepository.insertPlaylist(playlist.info.copy(coverPath = playlistImage?.path))
                }
            }

            try {
                var fullSong: Song? = null
                songRepository.getSongInfo(song.youtubeId).collect { apiResult ->
                    if (apiResult is ApiResult.Success) {
                        fullSong = apiResult.data
                    }
                }

                // 1. Download the thumbnail FIRST so we can display it in the progressive notification
                val thumbnailPath = DownloadHelper.downloadImage(
                    appContext,
                    fullSong?.thumbnailHref ?: song.thumbnailHref,
                    song.youtubeId
                )
                val songWithThumb = song.copy(thumbnailPath = thumbnailPath?.path)

                // 2. Put the worker in the Foreground to show the rich notification immediately
                setForeground(createForegroundInfo(songWithThumb, notificationId, progress = 0, indeterminate = true))

                // 3. Download the audio. 
                // NOTE: If DownloadHelper supports a progress callback, you can update the notification here.
                val audioPath = DownloadHelper.downloadAudio(
                    appContext, songWithThumb, connections = 8, persistPublicly = persistPublicly
                )

                val updatedSong = songWithThumb.copy(audioFilePath = audioPath)
                localSongRepository.create(updatedSong)

                ensureYoutubeSongInLibrary(updatedSong)

                if (audioPath != null) {
                    val mainId = -(15_000_000_000_000L + song.youtubeId.hashCode().toLong().absoluteValue)
                    val parentDir = if (audioPath.startsWith("content://")) "Music/PixelMusic" else File(audioPath).parentFile?.absolutePath ?: ""
                    musicDao.updateSongFilePathAndParent(mainId, audioPath, parentDir)
                }

                // Remove the foreground progress notification, and show the final success one
                if (persistPublicly) {
                    notificationManager.cancel(notificationId)
                    UmihiNotificationManager.showSongDownloadSuccess(appContext, song)
                }
                Result.success()
                
            } catch (_: CancellationException) {
                notificationManager.cancel(notificationId)
                UmihiHelper.printd("Song download canceled ${song.title}")
                Result.failure()
            } catch (e: Exception) {
                notificationManager.cancel(notificationId)
                if (persistPublicly) {
                    UmihiNotificationManager.showSongDownloadFailed(appContext, song)
                }
                Result.failure()
            }
        }
    }

    /**
     * Builds the rich, Material You progressive notification.
     */
    private fun createForegroundInfo(song: Song, notificationId: Int, progress: Int, indeterminate: Boolean): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Downloads",
                NotificationManager.IMPORTANCE_LOW // Low importance prevents it from popping over the screen aggressively
            ).apply { description = "Shows progress for downloading songs" }
            notificationManager.createNotificationChannel(channel)
        }

        // Load the downloaded thumbnail as a Bitmap for the Large Icon
        val albumArtBitmap = song.thumbnailPath?.let { path ->
            BitmapFactory.decodeFile(path)
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText("Downloading • ${song.artist}")
            .setSmallIcon(android.R.drawable.stat_sys_download) // Fallback system icon
            .setLargeIcon(albumArtBitmap) // Injects the album art!
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)
            .build()

        // Required for Android 14+ Foreground Service types
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    // ... (Keep toUnifiedYoutubeSongId, toUnifiedYoutubeAlbumId, toUnifiedYoutubeArtistId exactly as they were) ...
    private fun toUnifiedYoutubeSongId(youtubeId: String): Long = -(15_000_000_000_000L + youtubeId.hashCode().toLong().absoluteValue)
    private fun toUnifiedYoutubeAlbumId(albumName: String): Long = -(16_000_000_000_000L + albumName.lowercase().hashCode().toLong().absoluteValue)
    private fun toUnifiedYoutubeArtistId(artistName: String): Long = -(17_000_000_000_000L + artistName.lowercase().hashCode().toLong().absoluteValue)

    private fun parseYoutubeArtistNames(artistStr: String): List<String> {
        if (artistStr.isBlank()) return listOf("Unknown Artist")
        val parsed = artistStr
            .split(Regex("\\s*[,/&;+、•]\\s*|\\s+(?:feat\\.|ft\\.|vs)\\s+|\\s+and\\s+", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        return if (parsed.isEmpty()) listOf("Unknown Artist") else parsed
    }

    private suspend fun ensureYoutubeSongInLibrary(song: Song) {
        val songId = toUnifiedYoutubeSongId(song.youtubeId)
        val title = song.title.takeIf { it.isNotBlank() } ?: "YouTube Video"
        val artist = song.artist.takeIf { it.isNotBlank() } ?: "Unknown Artist"
        val artistNames = parseYoutubeArtistNames(artist)
        val primaryArtistName = artistNames.firstOrNull() ?: "Unknown Artist"
        val primaryArtistId = toUnifiedYoutubeArtistId(primaryArtistName)

        val artistsToInsert = artistNames.map { name ->
            com.unshoo.pixelmusic.data.database.ArtistEntity(id = toUnifiedYoutubeArtistId(name), name = name, trackCount = 0, imageUrl = null)
        }

        val crossRefsToInsert = artistNames.mapIndexed { index, name ->
            com.unshoo.pixelmusic.data.database.SongArtistCrossRef(songId = songId, artistId = toUnifiedYoutubeArtistId(name), isPrimary = index == 0)
        }

        val albumId = toUnifiedYoutubeAlbumId("YouTube Music")
        val albumName = "YouTube Music"
        val albumToInsert = com.unshoo.pixelmusic.data.database.AlbumEntity(
            id = albumId, title = albumName, artistName = primaryArtistName, artistId = primaryArtistId,
            songCount = 0, dateAdded = System.currentTimeMillis(), year = 0, albumArtUriString = song.thumbnailPath ?: song.thumbnailHref
        )

        val artistsJson = try {
            val arr = org.json.JSONArray()
            artistNames.forEachIndexed { idx, name ->
                val obj = org.json.JSONObject()
                obj.put("id", toUnifiedYoutubeArtistId(name))
                obj.put("name", name)
                obj.put("primary", idx == 0)
                arr.put(obj)
            }
            arr.toString()
        } catch (e: Exception) { null }

        val durationMs = try {
            if (song.duration.contains(":")) {
                val parts = song.duration.split(":")
                when (parts.size) {
                    1 -> parts[0].toLong() * 1000L
                    2 -> (parts[0].toLong() * 60L + parts[1].toLong()) * 1000L
                    3 -> ((parts[0].toLong() * 3600L + parts[1].toLong() * 60L + parts[2].toLong())) * 1000L
                    else -> 0L
                }
            } else {
                song.duration.toLongOrNull() ?: 0L
            }
        } catch (e: Exception) { 0L }

        val songEntity = com.unshoo.pixelmusic.data.database.SongEntity(
            id = songId, title = title, artistName = artist, artistId = primaryArtistId, albumArtist = null,
            albumName = albumName, albumId = albumId, contentUriString = "youtube://${song.youtubeId}",
            albumArtUriString = song.thumbnailPath ?: song.thumbnailHref, duration = durationMs,
            genre = song.genre?.takeIf { it.isNotBlank() } ?: "YouTube Music", filePath = "", parentDirectoryPath = "youtube://",
            isFavorite = false, lyrics = null, trackNumber = 0, year = 0, dateAdded = System.currentTimeMillis(),
            mimeType = "audio/webm", bitrate = null, sampleRate = null, telegramChatId = null, telegramFileId = null,
            artistsJson = artistsJson, sourceType = com.unshoo.pixelmusic.data.database.SourceType.YOUTUBE
        )

        musicDao.incrementalSyncMusicData(listOf(songEntity), listOf(albumToInsert), artistsToInsert, crossRefsToInsert, emptyList())
    }

    companion object {
        const val PLAYLIST_KEY = "playlist"
        const val SONG_KEY = "song"
    }
}
