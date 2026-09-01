package com.unshoo.pixelmusic.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.unshoo.pixelmusic.data.model.Song
import com.unshoo.pixelmusic.data.remote.youtube.YoutubeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.StandardArtwork
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.util.Locale

object SongDownloader {

    private const val CHANNEL_ID = "pixelmusic_download_channel"
    private const val CHUNK_SIZE = 5 * 1024 * 1024L // 5MB throttle-bypass chunks

    suspend fun downloadAndTagSong(
        context: Context,
        song: Song,
        lyricsText: String? = null
    ): Boolean = withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
        var tempAudioFile: File? = null
        var tempRemuxedFile: File? = null
        var tempImageFile: File? = null

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (song.youtubeId ?: song.id).hashCode()

        createNotificationChannel(notificationManager)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(song.title)
            .setContentText("Connecting...")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, true)

        try {
            updateNotification(notificationManager, notificationId, notificationBuilder)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Downloading ${song.title}...", Toast.LENGTH_SHORT).show()
            }

            // 1. Resolve stream URL
            val ytSong = com.unshoo.pixelmusic.data.model.youtube.Song(
                youtubeId = song.youtubeId ?: song.id.removePrefix("youtube_"),
                title = song.title,
                artist = song.displayArtist,
                duration = "",
                thumbnailHref = song.albumArtUriString ?: ""
            )
            val streamUrl = YoutubeHelper.getDownloadUrl(context, ytSong)
            if (streamUrl.isBlank()) throw Exception("Could not resolve stream URL")

            val cleanTitle = song.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val cleanArtist = song.displayArtist.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val fileName = "$cleanTitle - $cleanArtist.m4a"

            tempAudioFile = File(context.cacheDir, "raw_$fileName")
            tempRemuxedFile = File(context.cacheDir, "clean_$fileName")
            tempImageFile = File(context.cacheDir, "temp_cover_${System.currentTimeMillis()}.jpg")

            // 2. Concurrently download album artwork via native HttpURLConnection
            val imageDownloadJob = async(Dispatchers.IO) {
                if (!song.albumArtUriString.isNullOrBlank()) {
                    try {
                        val url = URL(song.albumArtUriString!!)
                        val connection = (url.openConnection() as HttpURLConnection).apply {
                            connectTimeout = 15000
                            readTimeout = 15000
                            instanceFollowRedirects = true
                        }
                        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                            connection.inputStream.use { input ->
                                FileOutputStream(tempImageFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        connection.disconnect()
                    } catch (_: Exception) {
                        // Non-fatal if cover art fetch fails
                    }
                }
            }

            // 3. Download audio file in 5MB chunks via HttpURLConnection with accurate live progress
            var startByte = 0L
            var totalBytes = -1L
            var totalDownloaded = 0L
            var isFinished = false
            var lastNotificationUpdateTime = 0L

            FileOutputStream(tempAudioFile).use { output ->
                while (!isFinished) {
                    val endByte = startByte + CHUNK_SIZE - 1
                    val connection = (URL(streamUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 60000
                        requestMethod = "GET"
                        setRequestProperty("Range", "bytes=$startByte-$endByte")
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
                        setRequestProperty("Origin", "https://music.youtube.com")
                        setRequestProperty("Referer", "https://music.youtube.com/")
                        instanceFollowRedirects = true
                    }

                    try {
                        connection.connect()
                        val responseCode = connection.responseCode
                        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                            throw Exception("Chunk download failed. HTTP Code: $responseCode")
                        }

                        // Determine total file size from Content-Range header (e.g. "bytes 0-5242879/12582912")
                        if (totalBytes <= 0) {
                            val contentRange = connection.getHeaderField("Content-Range")
                            if (contentRange != null && contentRange.contains("/")) {
                                totalBytes = contentRange.substringAfterLast("/").trim().toLongOrNull() ?: -1L
                            }
                            if (totalBytes <= 0) {
                                totalBytes = connection.contentLengthLong
                            }
                        }

                        val inputStream = connection.inputStream
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        var chunkReadTotal = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            chunkReadTotal += bytesRead
                            totalDownloaded += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastNotificationUpdateTime > 250L) { // Throttle updates to 4Hz to prevent UI lag
                                lastNotificationUpdateTime = now
                                updateLiveProgress(
                                    notificationManager,
                                    notificationId,
                                    notificationBuilder,
                                    totalDownloaded,
                                    totalBytes
                                )
                            }
                        }

                        if (chunkReadTotal < CHUNK_SIZE) {
                            isFinished = true
                        }
                        startByte += chunkReadTotal
                        inputStream.close()
                    } finally {
                        connection.disconnect()
                    }
                }
                output.flush()
            }

            // Ensure final 100% download state is reflected
            updateLiveProgress(
                notificationManager,
                notificationId,
                notificationBuilder,
                totalDownloaded,
                if (totalBytes > 0) totalBytes else totalDownloaded
            )

            // 4. Remux DASH M4A to Standard M4A
            notificationBuilder
                .setContentText("Processing audio file...")
                .setProgress(100, 100, true)
            updateNotification(notificationManager, notificationId, notificationBuilder)

            val extractor = MediaExtractor().apply {
                setDataSource(tempAudioFile.absolutePath)
            }

            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex >= 0) {
                extractor.selectTrack(audioTrackIndex)
                val format = extractor.getTrackFormat(audioTrackIndex)

                val muxer = MediaMuxer(tempRemuxedFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val muxerTrackIndex = muxer.addTrack(format)
                muxer.start()

                val buffer = ByteBuffer.allocateDirect(1024 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.flags = extractor.sampleFlags
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }

                muxer.stop()
                muxer.release()
                extractor.release()
            } else {
                extractor.release()
                throw Exception("No audio track found in downloaded file")
            }

            imageDownloadJob.await()

            // 5. Embed Metadata using jaudiotagger
            notificationBuilder.setContentText("Writing metadata...")
            updateNotification(notificationManager, notificationId, notificationBuilder)

            val audioFile = AudioFileIO.read(tempRemuxedFile)
            val tag = audioFile.tagOrCreateAndSetDefault

            tag.setField(FieldKey.TITLE, song.title)
            tag.setField(FieldKey.ARTIST, song.displayArtist)
            if (!song.album.isNullOrBlank()) {
                tag.setField(FieldKey.ALBUM, song.album)
            }
            if (!lyricsText.isNullOrBlank()) {
                tag.setField(FieldKey.LYRICS, lyricsText)
            }
            if (tempImageFile.exists() && tempImageFile.length() > 0) {
                val artwork = StandardArtwork.createArtworkFromFile(tempImageFile)
                tag.setField(artwork)
            }

            audioFile.commit()

            // 6. Save to Public MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.TITLE, song.title)
                put(MediaStore.Audio.Media.ARTIST, song.displayArtist)
                if (!song.album.isNullOrBlank()) {
                    put(MediaStore.Audio.Media.ALBUM, song.album)
                }
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/PixelMusic")
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Failed to create MediaStore entry")

            resolver.openOutputStream(uri)?.use { outputStream ->
                tempRemuxedFile.inputStream().use { inputStream ->
                    val copyBuffer = ByteArray(64 * 1024)
                    var readCount: Int
                    while (inputStream.read(copyBuffer).also { readCount = it } != -1) {
                        outputStream.write(copyBuffer, 0, readCount)
                    }
                    outputStream.flush()
                }
            }

            notificationBuilder
                .setContentText("Download complete (${formatMb(totalDownloaded)})")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
            updateNotification(notificationManager, notificationId, notificationBuilder)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved to Music/PixelMusic!", Toast.LENGTH_LONG).show()
            }

            return@withContext true

        } catch (e: Exception) {
            e.printStackTrace()

            notificationBuilder
                .setContentText("Download failed: ${e.message ?: "Unknown error"}")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
            updateNotification(notificationManager, notificationId, notificationBuilder)

            withContext(Dispatchers.Main) {
                val errorMsg = e.message ?: "Unknown error"
                Toast.makeText(context, "Download failed: $errorMsg", Toast.LENGTH_LONG).show()
            }
            return@withContext false
        } finally {
            // 7. Cleanup
            tempAudioFile?.takeIf { it.exists() }?.delete()
            tempRemuxedFile?.takeIf { it.exists() }?.delete()
            tempImageFile?.takeIf { it.exists() }?.delete()
        }
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Song Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live download progress for songs"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateLiveProgress(
        notificationManager: NotificationManager,
        notificationId: Int,
        builder: NotificationCompat.Builder,
        currentBytes: Long,
        totalBytes: Long
    ) {
        if (totalBytes > 0) {
            val progressPercent = ((currentBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
            val currentMb = formatMb(currentBytes)
            val totalMb = formatMb(totalBytes)
            builder
                .setContentText("$currentMb / $totalMb ($progressPercent%)")
                .setProgress(100, progressPercent, false)
        } else {
            builder
                .setContentText("${formatMb(currentBytes)} downloaded")
                .setProgress(0, 0, true)
        }
        updateNotification(notificationManager, notificationId, builder)
    }

    private fun updateNotification(
        notificationManager: NotificationManager,
        notificationId: Int,
        builder: NotificationCompat.Builder
    ) {
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled safely if POST_NOTIFICATIONS runtime permission has not been granted yet
        }
    }

    private fun formatMb(bytes: Long): String {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
