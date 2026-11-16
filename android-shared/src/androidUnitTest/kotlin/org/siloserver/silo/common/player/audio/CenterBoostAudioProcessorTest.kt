package org.siloserver.silo.common.player.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.StreamMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CenterBoostAudioProcessorTest {

    private fun frames(channels: Int, vararg samples: Short): ByteBuffer {
        val b = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder())
        samples.forEach { b.putShort(it) }
        b.flip()
        return b
    }

    private fun CenterBoostAudioProcessor.run(channels: Int, input: ByteBuffer): List<Int> {
        configure(AudioFormat(48_000, channels, C.ENCODING_PCM_16BIT))
        flush(StreamMetadata.DEFAULT)
        queueInput(input)
        val out = output.order(ByteOrder.nativeOrder())
        return List(out.remaining() / 2) { out.getShort().toInt() }
    }

    @Test
    fun `boosts only the centre channel of a 5_1 frame`() {
        val p = CenterBoostAudioProcessor()
        p.setGainDb(6)
        val out = p.run(6, frames(6, 1000, 1000, 1000, 1000, 1000, 1000))
        assertEquals(listOf(1000, 1000, 1995, 1000, 1000, 1000), out)
    }

    @Test
    fun `clips instead of wrapping`() {
        val p = CenterBoostAudioProcessor()
        p.setGainDb(9)
        val out = p.run(6, frames(6, 0, 0, 20_000, 0, 0, 0))
        assertEquals(32_767, out[2])
    }

    @Test
    fun `off is a pass through`() {
        val p = CenterBoostAudioProcessor()
        val out = p.run(6, frames(6, 1, 2, 3, 4, 5, 6))
        assertEquals(listOf(1, 2, 3, 4, 5, 6), out)
    }

    @Test
    fun `stereo stays out of the chain`() {
        val p = CenterBoostAudioProcessor()
        p.setGainDb(9)
        p.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        assertFalse(p.isActive)
    }

    @Test
    fun `a frame split across buffers keeps its channels`() {
        val p = CenterBoostAudioProcessor()
        p.setGainDb(6)
        p.configure(AudioFormat(48_000, 3, C.ENCODING_PCM_16BIT))
        p.flush(StreamMetadata.DEFAULT)
        assertTrue(p.isActive)
        p.queueInput(frames(3, 1000, 1000))
        val first = p.output.order(ByteOrder.nativeOrder())
        assertEquals(2, first.remaining() / 2)
        p.queueInput(frames(3, 1000))
        val second = p.output.order(ByteOrder.nativeOrder())
        assertEquals(1995, second.getShort().toInt())
    }
}
