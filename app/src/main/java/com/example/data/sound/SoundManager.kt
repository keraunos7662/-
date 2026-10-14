package com.example.data.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.SoundType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Realistic procedural sound synthesizer and player for Criminal Russia 90s.
 * Generates genuine 16-bit 44.1kHz PCM acoustics for gunshots, punches, cash registers,
 * militia sirens, card deals, and UI clicks without relying on telephone ToneGenerator beeps.
 */
class SoundManager(private val context: Context) {

    private val executor = Executors.newFixedThreadPool(2)
    private val pcmCache = ConcurrentHashMap<SoundType, ByteArray>()
    private val trackCache = ConcurrentHashMap<SoundType, AudioTrack>()

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    init {
        // Pre-generate PCM sound buffers asynchronously
        executor.execute {
            for (type in SoundType.entries) {
                try {
                    val pcm = synthesizeSound(type)
                    pcmCache[type] = pcm
                    initStaticTrack(type, pcm)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun initStaticTrack(type: SoundType, pcm: ByteArray) {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(pcm.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            trackCache[type] = track
        } catch (_: Exception) {
        }
    }

    fun playSound(type: SoundType) {
        executor.execute {
            try {
                val track = trackCache[type]
                if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                    try {
                        track.stop()
                        track.reloadStaticData()
                        track.play()
                    } catch (_: Exception) {
                        playPcmFallback(type)
                    }
                } else {
                    playPcmFallback(type)
                }
            } catch (_: Exception) {
            }

            // Accompany with tactile vibration
            triggerHaptic(type)
        }
    }

    private fun playPcmFallback(type: SoundType) {
        val pcm = pcmCache[type] ?: synthesizeSound(type).also { pcmCache[type] = it }
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val streamTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(pcm.size)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            streamTrack.play()
            streamTrack.write(pcm, 0, pcm.size)
            streamTrack.stop()
            streamTrack.release()
        } catch (_: Exception) {
        }
    }

    private fun triggerHaptic(type: SoundType) {
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    val duration = when (type) {
                        SoundType.CLICK -> 18L
                        SoundType.CARD -> 25L
                        SoundType.CASH -> 45L
                        SoundType.PUNCH -> 80L
                        SoundType.SHOT -> 130L
                        SoundType.SIREN -> 250L
                        SoundType.VICTORY -> 120L
                        SoundType.RAGE -> 180L
                        SoundType.PHONE -> 60L
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(duration)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        executor.shutdown()
        trackCache.values.forEach { track ->
            try {
                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }
        trackCache.clear()
        pcmCache.clear()
    }

    companion object {
        private const val SAMPLE_RATE = 44100

        private fun synthesizeSound(type: SoundType): ByteArray {
            return when (type) {
                SoundType.CLICK -> generateClick()
                SoundType.CASH -> generateCashRegister()
                SoundType.PUNCH -> generatePunch()
                SoundType.SHOT -> generateGunshot()
                SoundType.SIREN -> generatePoliceSiren()
                SoundType.VICTORY -> generateVictoryFanfare()
                SoundType.CARD -> generateCardSlide()
                SoundType.RAGE -> generateRageBoom()
                SoundType.PHONE -> generatePhoneRing()
            }
        }

        /**
         * Crisp mechanical button click / weapon toggle
         */
        private fun generateClick(): ByteArray {
            val durationMs = 28
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-130.0 * t)
                val tone = sin(2.0 * PI * 1900.0 * t) * 0.7
                val noise = if (i < 40) (Random.nextDouble() * 2.0 - 1.0) * 0.3 else 0.0
                val sample = ((tone + noise) * env).coerceIn(-1.0, 1.0)
                buffer.putShort((sample * 30000).toInt().toShort())
            }
            return buffer.array()
        }

        /**
         * Authentic metallic cash register chime & coin clinking
         */
        private fun generateCashRegister(): ByteArray {
            val durationMs = 420
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE

                // Bell 1: 1760 Hz (A6) with 2nd harmonic
                val bell1Env = exp(-9.0 * t)
                val bell1 = (sin(2.0 * PI * 1760.0 * t) * 0.5 + sin(2.0 * PI * 3520.0 * t) * 0.25) * bell1Env

                // Bell 2: starts at 55ms, 2637 Hz (E7) with bright harmonics (ka-ching)
                val t2 = t - 0.055
                val bell2 = if (t2 >= 0) {
                    val bell2Env = exp(-7.0 * t2)
                    (sin(2.0 * PI * 2637.0 * t2) * 0.6 + sin(2.0 * PI * 5274.0 * t2) * 0.2) * bell2Env
                } else {
                    0.0
                }

                // Coin clink transient noise in middle
                val t3 = t - 0.08
                val coins = if (t3 in 0.0..0.04) {
                    (Random.nextDouble() * 2.0 - 1.0) * 0.25 * exp(-60.0 * t3)
                } else {
                    0.0
                }

                val mixed = (bell1 + bell2 + coins).coerceIn(-1.0, 1.0)
                buffer.putShort((mixed * 31000).toInt().toShort())
            }
            return buffer.array()
        }

        /**
         * Heavy bone-crunching fist impact with sub-bass drop and crunch transient
         */
        private fun generatePunch(): ByteArray {
            val durationMs = 180
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE

                // Pitch drops sharply from 170 Hz to 45 Hz
                val freq = 45.0 + 125.0 * exp(-28.0 * t)
                phase += 2.0 * PI * freq / SAMPLE_RATE

                val bassEnv = exp(-14.0 * t)
                val bass = sin(phase) * bassEnv

                // Crunch transient in the first 25 ms
                val crunch = if (i < (SAMPLE_RATE * 0.025)) {
                    val crunchEnv = exp(-80.0 * t)
                    (Random.nextDouble() * 2.0 - 1.0) * 0.5 * crunchEnv
                } else {
                    0.0
                }

                // Soft saturation for visceral analog punch weight
                var total = bass * 0.85 + crunch * 0.4
                total = total.coerceIn(-1.0, 1.0)
                // Non-linear soft clip
                val saturated = if (total > 0) total - (total * total * 0.2) else total + (total * total * 0.2)

                buffer.putShort((saturated * 31500).toInt().toShort())
            }
            return buffer.array()
        }

        /**
         * Loud gunshot: explosive muzzle crack, low-end boom, and decaying echo
         */
        private fun generateGunshot(): ByteArray {
            val durationMs = 450
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

            var bassPhase = 0.0
            var filteredNoise = 0.0

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE

                // Blast noise with fast attack and steep exponential decay
                val rawNoise = Random.nextDouble() * 2.0 - 1.0
                filteredNoise = filteredNoise * 0.7 + rawNoise * 0.3 // lowpass smoothing
                val noiseEnv = exp(-13.0 * t)
                val blast = filteredNoise * noiseEnv

                // Heavy sub-bass thump (135 Hz -> 35 Hz)
                val freq = 35.0 + 100.0 * exp(-16.0 * t)
                bassPhase += 2.0 * PI * freq / SAMPLE_RATE
                val bassEnv = exp(-7.5 * t)
                val bass = sin(bassPhase) * bassEnv

                // Gun hammer click at the very start (first 3ms)
                val click = if (i < 130) {
                    (Random.nextDouble() * 2.0 - 1.0) * 0.8
                } else 0.0

                val mixed = (blast * 0.65 + bass * 0.55 + click * 0.3).coerceIn(-1.0, 1.0)
                buffer.putShort((mixed * 32000).toInt().toShort())
            }
            return buffer.array()
        }

        /**
         * Authentic wailing militia / police siren with megaphone harmonics
         */
        private fun generatePoliceSiren(): ByteArray {
            val durationMs = 850
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE

                // Siren sweeps between 650 Hz and 1050 Hz (2.2 Hz cycle)
                val sweep = sin(2.0 * PI * 2.2 * t)
                val freq = 850.0 + 200.0 * sweep
                phase += 2.0 * PI * freq / SAMPLE_RATE

                // Fundamental + 3rd harmonic for megaphone horn grit
                val siren = sin(phase) * 0.7 + sin(3.0 * phase) * 0.22 + sin(5.0 * phase) * 0.08

                // Fade in / fade out envelope
                val fadeIn = (t / 0.05).coerceAtMost(1.0)
                val fadeOut = ((0.85 - t) / 0.05).coerceIn(0.0, 1.0)
                val env = fadeIn * fadeOut

                val mixed = (siren * env).coerceIn(-1.0, 1.0)
                buffer.putShort((mixed * 29000).toInt().toShort())
            }
            return buffer.array()
        }

        /**
         * Triumphant criminal fanfare chord
         */
        private fun generateVictoryFanfare(): ByteArray {
            val durationMs = 900
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE

                // 4-step sequence: C5 (523Hz), E5 (659Hz), G5 (784Hz), C6 chord (1046Hz + 784Hz)
                val (f1, f2, env) = when {
                    t < 0.18 -> {
                        val subT = t
                        Triple(523.25, 1046.5, exp(-3.0 * subT))
                    }
                    t < 0.36 -> {
                        val subT = t - 0.18
                        Triple(659.25, 1318.5, exp(-3.0 * subT))
                    }
                    t < 0.54 -> {
                        val subT = t - 0.36
                        Triple(783.99, 1567.98, exp(-3.0 * subT))
                    }
                    else -> {
                        val subT = t - 0.54
                        val vibrato = 1.0 + 0.012 * sin(2.0 * PI * 6.0 * subT)
                        Triple(1046.50 * vibrato, 783.99 * vibrato, exp(-2.2 * subT))
                    }
                }

                val brass = sin(2.0 * PI * f1 * t) * 0.65 + sin(2.0 * PI * f2 * t) * 0.35
                val mixed = (brass * env).coerceIn(-1.0, 1.0)
                buffer.putShort((mixed * 30000).toInt().toShort())
            }
            return buffer.array()
        }

        /**
         * Crisp card flick / slide for Katran (Russian 21)
         */
        private fun generateCardSlide(): ByteArray {
            val durationMs = 65
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val noise = Random.nextDouble() * 2.0 - 1.0
                val flutter = sin(2.0 * PI * 2400.0 * t) * 0.4
                val env = exp(-55.0 * t)
                val mixed = ((noise * 0.6 + flutter) * env).coerceIn(-1.0, 1.0)
                buffer.putShort((mixed * 28000).toInt().toShort())
            }
            return buffer.array()
        }

        /**
         * Sub-bass rumble and impact for Rage KO
         */
        private fun generateRageBoom(): ByteArray {
            val durationMs = 500
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val freq = 30.0 + 55.0 * exp(-8.0 * t)
                phase += 2.0 * PI * freq / SAMPLE_RATE
                val bass = sin(phase) * exp(-5.0 * t)
                val grit = if (i < 1500) (Random.nextDouble() * 2.0 - 1.0) * 0.3 * exp(-30.0 * t) else 0.0
                val mixed = (bass * 0.85 + grit).coerceIn(-1.0, 1.0)
                buffer.putShort((mixed * 32000).toInt().toShort())
            }
            return buffer.array()
        }

        /**
         * 90s dual-tone telephone bell ring for boss calls
         */
        private fun generatePhoneRing(): ByteArray {
            val durationMs = 380
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val buffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // Dual tone 440 Hz + 480 Hz
                val ring = sin(2.0 * PI * 440.0 * t) * 0.5 + sin(2.0 * PI * 480.0 * t) * 0.5
                val tremolo = (sin(2.0 * PI * 20.0 * t) * 0.4 + 0.6)
                val env = exp(-2.5 * t)
                val mixed = (ring * tremolo * env).coerceIn(-1.0, 1.0)
                buffer.putShort((mixed * 28000).toInt().toShort())
            }
            return buffer.array()
        }
    }
}
