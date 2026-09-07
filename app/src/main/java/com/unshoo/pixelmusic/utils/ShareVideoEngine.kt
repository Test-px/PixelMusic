package com.unshoo.pixelmusic.utils

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

object ShareVideoEngine {

    suspend fun createInstagramShareVideo(
        context: Context,
        imagePath: String,
        audioPath: String,
        outputPath: String
    ): Boolean = withContext(Dispatchers.Main) {
        val outputFile = File(outputPath)
        if (outputFile.exists()) outputFile.delete()

        return@withContext suspendCancellableCoroutine { continuation ->
            try {
                // 1. Prepare the static image track (15 seconds at 30fps)
                val imageMediaItem = MediaItem.fromUri(Uri.parse("file://$imagePath"))
                val editedImage = EditedMediaItem.Builder(imageMediaItem)
                    .setDurationUs(15_000_000L) // 15 seconds
                    .setFrameRate(30)
                    .build()
                
                val imageSequence = EditedMediaItemSequence.withVideoFrom(listOf(editedImage))

                // 2. Prepare the audio track (Handles both local files and streaming URLs)
                val audioUri = if (audioPath.startsWith("http")) {
                    Uri.parse(audioPath)
                } else {
                    Uri.parse("file://$audioPath")
                }

                val audioMediaItem = MediaItem.Builder()
                    .setUri(audioUri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(30_000L) // Start at the 30-second mark (the hook)
                            .setEndPositionMs(45_000L)   // Grab exactly 15 seconds
                            .build()
                    )
                    .build()
                val editedAudio = EditedMediaItem.Builder(audioMediaItem).build()
                
                val audioSequence = EditedMediaItemSequence.withAudioFrom(listOf(editedAudio))

                // 3. Combine them into a single hardware-accelerated composition
                val composition = Composition.Builder(listOf(imageSequence, audioSequence)).build()

                // 4. Configure Transformer for MP4 encoding
                val transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (continuation.isActive) continuation.resume(true)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            exportException.printStackTrace()
                            if (continuation.isActive) continuation.resume(false)
                        }
                    })
                    .build()

                // 5. Start the render
                transformer.start(composition, outputPath)

                // Cancel if the coroutine dies
                continuation.invokeOnCancellation {
                    transformer.cancel()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (continuation.isActive) continuation.resume(false)
            }
        }
    }
}
