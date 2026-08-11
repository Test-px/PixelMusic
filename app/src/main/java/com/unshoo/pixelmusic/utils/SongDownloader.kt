package com.unshoo.pixelmusic.utils

import android.content.Context
import android.widget.Toast
import com.unshoo.pixelmusic.data.model.Song
import com.unshoo.pixelmusic.data.remote.youtube.YoutubeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object SongDownloader {

    suspend fun downloadSongTemp(context: Context, song: Song): File? = withContext(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Fetching ${song.title}...", Toast.LENGTH_SHORT).show()
            }

            // 1. Get the URL based on user's Download Quality setting (Already exists in your code!)
            val streamUrl = YoutubeHelper.getDownloadUrl(context, song)
            if (streamUrl.isBlank()) {
                throw Exception("Could not resolve stream URL")
            }

            // 2. Setup temporary file in the app's hidden cache
            val cleanTitle = song.title.replace("/", "_").replace("\\", "_")
            val cleanArtist = song.displayArtist.replace("/", "_").replace("\\", "_")
            val tempFile = File(context.cacheDir, "$cleanTitle - $cleanArtist - temp.m4a")

            // 3. Download the file using your existing OkHttp client
            val request = Request.Builder()
                .url(streamUrl)
                // Using the exact User-Agent from your extractor
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; rv:78.0) Gecko/20100101 Firefox/78.0")
                .build()

            val response = YoutubeHelper.client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Failed to download: ${response.code}")

            response.body?.let { body ->
                FileOutputStream(tempFile).use { output ->
                    body.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Raw download complete! Ready for metadata.", Toast.LENGTH_SHORT).show()
            }

            return@withContext tempFile

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            return@withContext null
        }
    }
}

