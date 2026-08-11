package com.unshoo.pixelmusic.utils

import android.content.ContentValues
import android.content.Context
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

object SongDownloader {

    /**
     * Downloads the stream, embeds metadata/cover art, and moves it to the public Music folder.
     */
    suspend fun downloadAndTagSong(
        context: Context, 
        song: Song, 
        lyricsText: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        var tempAudioFile: File? = null
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

            // Strip illegal characters for file naming
            val cleanTitle = song.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val cleanArtist = song.displayArtist.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val fileName = "$cleanTitle - $cleanArtist.m4a"
            
            tempAudioFile = File(context.cacheDir, "temp_$fileName")
            tempImageFile = File(context.cacheDir, "temp_cover.jpg")

            // 2. Download the audio file
            val audioRequest = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; rv:78.0) Gecko/20100101 Firefox/78.0")
                .build()

            val audioResponse = YoutubeHelper.client.newCall(audioRequest).execute()
            if (!audioResponse.isSuccessful) throw Exception("Failed to download audio")

            audioResponse.body?.byteStream()?.use { input ->
                FileOutputStream(tempAudioFile).use { output ->
                    input.copyTo(output)
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Writing metadata...", Toast.LENGTH_SHORT).show()
            }

            // 3. Download the Album Art
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

            // 4. Embed Metadata using jaudiotagger
            val audioFile = AudioFileIO.read(tempAudioFile)
            val tag = audioFile.tagOrCreateAndSetDefault
            
            tag.setField(FieldKey.TITLE, song.title)
            tag.setField(FieldKey.ARTIST, song.displayArtist)
            if (!song.album.isNullOrBlank()) {
                tag.setField(FieldKey.ALBUM, song.album)
            }
            
            // Embed lyrics if available
            if (!lyricsText.isNullOrBlank()) {
                tag.setField(FieldKey.LYRICS, lyricsText)
            }

            // Embed cover art if successfully downloaded
            if (tempImageFile.exists()) {
                val artwork = StandardArtwork.createArtworkFromFile(tempImageFile)
                tag.setField(artwork)
            }

            audioFile.commit()

            // 5. Move to Public MediaStore (Music/PixelMusic)
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4") // m4a is the mp4 container
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
                tempAudioFile.inputStream().use { inputStream ->
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
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            return@withContext false
        } finally {
            // 6. Cleanup temp files to save space
            tempAudioFile?.takeIf { it.exists() }?.delete()
            tempImageFile?.takeIf { it.exists() }?.delete()
        }
    }
}
