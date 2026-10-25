package com.letitout.app.ui

import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/** Converts Android TTS PCM16 chunks into the 16 kHz mono stream expected by Simli. */
internal class Pcm16Resampler(private val sampleRate: Int, private val channels: Int) {
    init {
        require(sampleRate in 8_000..96_000)
        require(channels in 1..2)
    }

    private val frameBytes = channels * 2
    private var carry = ByteArray(0)
    private var inputFrames = 0L
    private var previous = 0.0
    private var nextOutputPosition = 0.0
    private val inputFramesPerOutput = sampleRate / 16_000.0

    fun convert(chunk: ByteArray): ByteArray {
        if (chunk.isEmpty()) return ByteArray(0)
        val data = if (carry.isEmpty()) chunk else carry + chunk
        val complete = data.size / frameBytes * frameBytes
        carry = data.copyOfRange(complete, data.size)
        val out = ByteArrayOutputStream(complete * 16_000 / sampleRate / channels + 4)
        var offset = 0
        while (offset < complete) {
            var sum = 0
            repeat(channels) { channel ->
                val index = offset + channel * 2
                val value = (data[index].toInt() and 0xff) or (data[index + 1].toInt() shl 8)
                sum += value.toShort().toInt()
            }
            val sample = sum.toDouble() / channels
            val index = inputFrames.toDouble()
            while (nextOutputPosition <= index + 1e-9) {
                val value = if (inputFrames == 0L) sample else {
                    val fraction = (nextOutputPosition - (index - 1.0)).coerceIn(0.0, 1.0)
                    previous + (sample - previous) * fraction
                }
                val pcm = value.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                out.write(pcm and 0xff)
                out.write((pcm ushr 8) and 0xff)
                nextOutputPosition += inputFramesPerOutput
            }
            previous = sample
            inputFrames++
            offset += frameBytes
        }
        return out.toByteArray()
    }
}
