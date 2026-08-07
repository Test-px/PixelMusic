/*
 * Ported for PixelMusic
 * GPL-3.0 License
 */

package com.unshoo.pixelmusic.data.shazam

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MusicRecognizer {

    @SuppressLint("MissingPermission")
    suspend fun recognizeCurrentAudio(): Result<RecognitionResult> = withContext(Dispatchers.IO) {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            return@withContext Result.failure(Exception("Failed to initialize AudioRecord"))
        }

        val generator = ShazamSignatureGenerator()
        val buffer = ShortArray(bufferSize)

        audioRecord.startRecording()

        try {
            var signature: ShazamSignature? = null
            val startTime = System.currentTimeMillis()

            // Listen for up to 6 seconds to gather a clean audio signature
            while (signature == null && (System.currentTimeMillis() - startTime) < 6000) {
                val readSize = audioRecord.read(buffer, 0, buffer.size)
                if (readSize > 0) {
                    generator.feedPcm16Mono(buffer.copyOfRange(0, readSize))
                    signature = generator.nextSignatureOrNull()
                }
            }

            if (signature == null) {
                return@withContext Result.failure(Exception("Could not capture audio signature. Try getting closer to the sound."))
            }

            // Send signature to Shazam
            return@withContext Shazam.recognize(signature.uri, signature.sampleDurationMs)

        } finally {
            runCatching {
                audioRecord.stop()
                audioRecord.release()
            }
            generator.reset()
        }
    }
}

