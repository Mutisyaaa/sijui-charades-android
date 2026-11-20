package com.sijui.charades.hardware

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundAndHaptics(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var soundEnabled: Boolean = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun vibrate(durationMs: Long = 50L) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
        }
    }

    fun playCorrect() {
        vibrate(65L)
        if (!soundEnabled) return
        playSequence(listOf(587.33 to 90, 880.0 to 160))
    }

    fun playPass() {
        vibrate(40L)
        if (!soundEnabled) return
        playSequence(listOf(340.0 to 90, 230.0 to 130))
    }

    fun playTick(urgent: Boolean = false) {
        if (urgent) vibrate(25L)
        if (!soundEnabled) return
        playSequence(listOf((if (urgent) 920.0 else 640.0) to 65))
    }

    fun playCelebration() {
        vibrate(140L)
        if (!soundEnabled) return
        playSequence(
            listOf(
                523.25 to 110,
                659.25 to 110,
                783.99 to 110,
                1046.50 to 260,
            ),
        )
    }

    fun playWahWahRoast() {
        vibrate(180L)
        if (!soundEnabled) return
        playSequence(
            listOf(
                392.0 to 210,
                369.99 to 210,
                349.23 to 210,
                311.13 to 420,
            ),
        )
    }

    private fun playSequence(notes: List<Pair<Double, Int>>) {
        scope.launch {
            runCatching {
                val sampleRate = 22050
                val totalSamples = notes.sumOf { (sampleRate * it.second) / 1000 }
                val pcm = ShortArray(totalSamples)
                var offset = 0

                for ((freq, durationMs) in notes) {
                    val count = (sampleRate * durationMs) / 1000
                    for (i in 0 until count) {
                        val env = when {
                            i < 120 -> i / 120.0
                            i > count - 200 -> ((count - i).coerceAtLeast(0)) / 200.0
                            else -> 1.0
                        }
                        val sample = sin(2.0 * PI * i * freq / sampleRate) * env * 0.45
                        pcm[offset + i] = (sample * Short.MAX_VALUE).toInt().toShort()
                    }
                    offset += count
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                val totalMs = notes.sumOf { it.second }.toLong()
                kotlinx.coroutines.delay(totalMs + 100L)
                track.stop()
                track.release()
            }
        }
    }
}
