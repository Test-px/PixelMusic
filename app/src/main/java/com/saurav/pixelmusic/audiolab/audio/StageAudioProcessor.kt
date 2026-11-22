package com.saurav.pixelmusic.audiolab.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.saurav.pixelmusic.audiolab.AudioFxService
import com.saurav.pixelmusic.audiolab.stage.StageNative
import com.saurav.pixelmusic.audiolab.stage.StageParams
import java.nio.ByteBuffer
import java.nio.ByteOrder

@UnstableApi
class StageAudioProcessor(private val service: AudioFxService) : BaseAudioProcessor() {

    private var sampleRate = 0
    private var channels = 0
    private var handle = 0L

    private var directCopy: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    private var pushed: StageParams? = null
    private var idle = true

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || !StageNative.loaded) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return when (inputAudioFormat.channelCount) {
            2 -> inputAudioFormat
            in 3..8 -> if (service.stage.enabled) {
                AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, 2, C.ENCODING_PCM_16BIT)
            } else {
                AudioProcessor.AudioFormat.NOT_SET
            }
            else -> AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytes = inputBuffer.remaining()
        if (bytes == 0) return
        val input = inputBuffer.order(ByteOrder.nativeOrder())
        if (handle == 0L || channels <= 0) {
            if (inputAudioFormat.channelCount == outputAudioFormat.channelCount) {
                val out = replaceOutputBuffer(bytes)
                out.put(input)
                out.flip()
            } else {
                val inCh = inputAudioFormat.channelCount.coerceAtLeast(1)
                val frames = bytes / (2 * inCh)
                val out = replaceOutputBuffer(frames * 4)
                for (f in 0 until frames) {
                    val l = input.getShort()
                    val r = if (inCh >= 2) input.getShort() else l
                    for (c in 2 until inCh) input.getShort()
                    out.putShort(l)
                    out.putShort(r)
                }
                if (input.hasRemaining()) input.position(input.limit())
                out.flip()
            }
            return
        }
        val params = service.stage
        if (params !== pushed) {
            StageNative.setParams(handle, params.values)
            pushed = params
        }
        val frames = bytes / (2 * channels)
        if (frames == 0) {
            if (channels == 2) {
                val out = replaceOutputBuffer(bytes)
                out.put(input)
                out.flip()
            } else {
                input.position(input.limit())
            }
            return
        }
        if (channels == 2 && idle && !params.enabled) {
            val out = replaceOutputBuffer(bytes)
            out.put(input)
            out.flip()
            return
        }

        val source = if (input.isDirect) input else copyToDirect(input)
        val out = replaceOutputBuffer(frames * 4)
        idle = StageNative.process(handle, source, source.position(), out, frames)
        out.position(0)
        out.limit(frames * 4)
        input.position(input.position() + frames * 2 * channels)
        if (!idle) StageNative.levels(handle, service.stageLevels)
    }

    private fun copyToDirect(input: ByteBuffer): ByteBuffer {
        val size = input.remaining()
        if (directCopy.capacity() < size) {
            directCopy = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        }
        directCopy.clear()
        directCopy.put(input.duplicate())
        directCopy.flip()
        return directCopy
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        val format = inputAudioFormat
        if (format == AudioProcessor.AudioFormat.NOT_SET) return
        if (handle == 0L || format.sampleRate != sampleRate || format.channelCount != channels) {
            release()
            sampleRate = format.sampleRate
            channels = format.channelCount
            handle = StageNative.create(sampleRate, channels)
        } else {
            StageNative.clear(handle)
        }
        pushed = null
        idle = true
    }

    override fun onReset() {
        release()
        sampleRate = 0
        channels = 0
        pushed = null
        idle = true
        directCopy = AudioProcessor.EMPTY_BUFFER
    }

    private fun release() {
        if (handle != 0L) StageNative.destroy(handle)
        handle = 0L
    }
}
