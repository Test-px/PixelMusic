package com.unshoo.pixelmusic.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.unshoo.pixelmusic.data.model.Song
import com.unshoo.pixelmusic.data.remote.youtube.YoutubeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.StandardArtwork
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

object SongDownloader {

    suspend fun downloadAndTagSong(
        context: Context,
        song: Song,
        lyricsText: String? = null
    ): Boolean = withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
        var tempAudioFile: File? = null
        var tempRemuxedFile: File? = null
        var tempImageFile: File? = null

        try {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Downloading ${song.title}...", Toast.LENGTH_SHORT).show()
            }

            // 1. Get the stream URL honoring the user's Quality settings
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
            tempImageFile = File(context.cacheDir, "temp_cover.jpg")

            // 2. Download the audio file with WEB_REMIX headers and unlimited call timeout
            val downloadClient = YoutubeHelper.client.newBuilder()
                .callTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS) // Disable 4s call timeout
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                .writeTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                .build()

            val audioRequest = Request.Builder()
                .get()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .build()

            val audioResponse = downloadClient.newCall(audioRequest).execute()
            if (!audioResponse.isSuccessful && audioResponse.code != 206) {
                throw Exception("Failed to download audio. Code: ${audioResponse.code}")
            }

            audioResponse.body?.byteStream()?.use { input ->
                FileOutputStream(tempAudioFile).use { output ->
                    input.copyTo(output)
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Processing audio file...", Toast.LENGTH_SHORT).show()
            }

            // 3. Remux DASH M4A to Standard M4A
            // YouTube sends fragmented/DASH MP4s which crash jaudiotagger.
            // We use Android's native MediaMuxer to repackage it instantly without quality loss.
            val extractor = MediaExtractor()
            extractor.setDataSource(tempAudioFile.absolutePath)

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

                val buffer = ByteBuffer.allocate(1024 * 1024)
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

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Writing metadata...", Toast.LENGTH_SHORT).show()
            }

            // 4. Download Album Art
            if (!song.albumArtUriString.isNullOrBlank()) {
                val imageRequest = Request.Builder().url(song.albumArtUriString!!).build()
                val imageResponse = YoutubeHelper.client.newCall(imageRequest).execute()
                if (imageResponse.isSuccessful) {
                    imageResponse.body?.byteStream()?.use { input ->
                        FileOutputStream(tempImageFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }

            // 5. Embed Metadata using jaudiotagger on the REMUXED file
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

            if (tempImageFile.exists()) {
                val artwork = StandardArtwork.createArtworkFromFile(tempImageFile)
                tag.setField(artwork)
            }

            audioFile.commit()

            // 6. Move to Public MediaStore
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
                    inputStream.copyTo(outputStream)
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved to Music/PixelMusic!", Toast.LENGTH_LONG).show()
            }

            return@withContext true

        } catch (e: Exception) {
            e.printStackTrace()
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
}
