package com.letitout.app.ui

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Pcm16ResamplerTest {
    @Test fun preservesSixteenKilohertzMonoAcrossOddChunks() {
        val input = pcm(0, 1000, -1000, 3000)
        val converter = Pcm16Resampler(16_000, 1)
        val output = converter.convert(input.copyOfRange(0, 3)) + converter.convert(input.copyOfRange(3, input.size))
        assertArrayEquals(input, output)
    }

    @Test fun downmixesStereoAndResamplesTwentyFourKilohertz() {
        val converter = Pcm16Resampler(24_000, 2)
        val input = pcm(1000, -1000, 2000, 0, 3000, 1000, 4000, 2000)
        val output = converter.convert(input)
        assertEquals(6, output.size)
        assertArrayEquals(pcm(0, 1500, 3000), output)
    }

    private fun pcm(vararg values: Int): ByteArray = values.flatMap {
        listOf((it and 0xff).toByte(), ((it ushr 8) and 0xff).toByte())
    }.toByteArray()
}
