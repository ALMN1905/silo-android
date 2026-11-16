package org.siloserver.silo.common.player.audio

import android.content.SharedPreferences
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.StreamMetadata
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * Raises the volume of the centre channel only, so dialogue stands out from
 * music and effects on stereo and 3.x setups (a 5.1 mix is downmixed by the
 * device, which keeps the centre quieter than the sides).
 *
 * Works on decoded PCM with a centre channel at index 2 (Android and FFmpeg
 * order: L, R, C, ...), that is 3, 5, 6, 7 or 8 channels, 16-bit or float. A
 * stereo or 4-channel track, and a bitstream sent to a receiver as passthrough,
 * are left alone. The gain changes live, with no flush.
 *
 * The chosen gain is device-local and kept in [prefs] when one is given.
 */
@UnstableApi
class CenterBoostAudioProcessor(private val prefs: SharedPreferences? = null) : BaseAudioProcessor() {

    private val _gainDb = MutableStateFlow(
        (prefs?.getInt(PREF_GAIN_DB, 0) ?: 0).let { if (it in GAIN_STEPS_DB) it else 0 },
    )

    /** The boost in dB, one of [GAIN_STEPS_DB]. */
    val gainDb: StateFlow<Int> = _gainDb

    @Volatile
    private var gain: Float = dbToGain(_gainDb.value)

    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID

    /** Channel of the next sample, carried across buffers so a split frame stays aligned. */
    private var channelCursor = 0
    private var scratch = ByteArray(0)

    fun setGainDb(db: Int) {
        val value = if (db in GAIN_STEPS_DB) db else 0
        prefs?.edit()?.putInt(PREF_GAIN_DB, value)?.apply()
        gain = dbToGain(value)
        _gainDb.value = value
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val supportedEncoding =
            inputAudioFormat.encoding == C.ENCODING_PCM_16BIT || inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        if (!supportedEncoding || inputAudioFormat.channelCount !in CHANNEL_COUNTS_WITH_CENTER) {
            // Not our format: stay out of the chain.
            return AudioFormat.NOT_SET
        }
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding
        return inputAudioFormat
    }

    override fun onFlush(streamMetadata: StreamMetadata) {
        channelCursor = 0
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        // Copy out first: the pipeline can hand this processor its own previous output as input.
        if (scratch.size < size) scratch = ByteArray(size)
        inputBuffer.get(scratch, 0, size)
        val out = replaceOutputBuffer(size).order(ByteOrder.nativeOrder())
        val input = ByteBuffer.wrap(scratch, 0, size).order(ByteOrder.nativeOrder())
        val g = gain
        if (encoding == C.ENCODING_PCM_FLOAT) {
            while (input.remaining() >= 4) {
                val sample = input.getFloat()
                out.putFloat(if (channelCursor == CENTER && g != 1f) (sample * g).coerceIn(-1f, 1f) else sample)
                channelCursor = (channelCursor + 1) % channelCount
            }
        } else {
            while (input.remaining() >= 2) {
                val sample = input.getShort().toInt()
                val scaled = if (channelCursor == CENTER && g != 1f) {
                    (sample * g).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                } else {
                    sample
                }
                out.putShort(scaled.toShort())
                channelCursor = (channelCursor + 1) % channelCount
            }
        }
        out.flip()
    }

    companion object {
        /** The steps the settings offer; 0 is off. */
        val GAIN_STEPS_DB = listOf(0, 3, 6, 9)
        private const val PREF_GAIN_DB = "gain_db"
        private const val CENTER = 2
        private val CHANNEL_COUNTS_WITH_CENTER = setOf(3, 5, 6, 7, 8)

        fun dbToGain(db: Int): Float = 10.0.pow(db / 20.0).toFloat()
    }
}
