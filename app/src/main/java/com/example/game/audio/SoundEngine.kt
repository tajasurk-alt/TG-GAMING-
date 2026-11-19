package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

class SoundEngine {
  @Volatile private var running = false
  @Volatile private var muted = false
  @Volatile private var inVehicle = false
  @Volatile private var isBike = false
  @Volatile private var rpmNorm = 0f
  @Volatile private var baseHz = 52f
  @Volatile private var drifting = false
  @Volatile private var sirenActive = false
  @Volatile private var chimeTimer = 0f

  private var audioThread: Thread? = null

  fun setMuted(isMuted: Boolean) {
    muted = isMuted
  }

  fun isMuted(): Boolean = muted

  fun updateTelemetry(
    inVehicle: Boolean,
    isBike: Boolean,
    rpmNorm: Float,
    baseHz: Float,
    drifting: Boolean,
    sirenActive: Boolean,
  ) {
    this.inVehicle = inVehicle
    this.isBike = isBike
    this.rpmNorm = rpmNorm.coerceIn(0f, 1f)
    this.baseHz = baseHz
    this.drifting = drifting
    this.sirenActive = sirenActive
  }

  fun triggerChime() {
    chimeTimer = 0.28f
  }

  fun start() {
    if (running) return
    running = true
    audioThread =
      thread(start = true, name = "ViceCoastAudio", isDaemon = true) {
        val sampleRate = 22050
        val bufferSamples = 512
        val minBufBytes =
          AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
          )
        val track =
          try {
            AudioTrack.Builder()
              .setAudioAttributes(
                AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_GAME)
                  .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                  .build()
              )
              .setAudioFormat(
                AudioFormat.Builder()
                  .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                  .setSampleRate(sampleRate)
                  .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                  .build()
              )
              .setBufferSizeInBytes(maxOf(minBufBytes, bufferSamples * 4))
              .setTransferMode(AudioTrack.MODE_STREAM)
              .build()
          } catch (_: Throwable) {
            running = false
            return@thread
          }

        try {
          track.play()
          val pcm = ShortArray(bufferSamples)
          var phase1 = 0.0
          var phase2 = 0.0
          var sirenPhase = 0.0
          var sirenMod = 0.0
          var chimePhase = 0.0
          var noiseSeed = 1337

          while (running) {
            if (muted) {
              pcm.fill(0)
              track.write(pcm, 0, pcm.size)
              Thread.sleep(20)
              continue
            }

            val dt = 1.0 / sampleRate
            val curInVeh = inVehicle
            val curBike = isBike
            val curRpm = rpmNorm
            val curBase = baseHz.toDouble()
            val curDrift = drifting
            val curSiren = sirenActive

            for (i in pcm.indices) {
              var sample = 0.0

              if (curInVeh) {
                val freq = curBase * (1.0 + curRpm * (if (curBike) 2.9 else 2.2))
                phase1 = (phase1 + freq * dt) % 1.0
                phase2 = (phase2 + freq * (if (curBike) 2.0 else 1.5) * dt) % 1.0
                val saw1 = 2.0 * phase1 - 1.0
                val sin2 = sin(2.0 * PI * phase2)
                val engineAmp = 0.09 + curRpm * 0.11
                sample += (saw1 * 0.55 + sin2 * 0.45) * engineAmp
              }

              if (curDrift) {
                noiseSeed = noiseSeed * 1664525 + 1013904223
                val n = ((noiseSeed ushr 16) and 0xFFFF) / 32768.0 - 1.0
                sample += n * 0.09
              }

              if (curSiren) {
                sirenMod = (sirenMod + 1.4 * dt) % 1.0
                val sFreq = 640.0 + sin(2.0 * PI * sirenMod) * 260.0
                sirenPhase = (sirenPhase + sFreq * dt) % 1.0
                sample += sin(2.0 * PI * sirenPhase) * 0.075
              }

              if (chimeTimer > 0f) {
                chimePhase = (chimePhase + 880.0 * dt) % 1.0
                sample += sin(2.0 * PI * chimePhase) * 0.14 * (chimeTimer / 0.28f)
                chimeTimer = (chimeTimer - dt.toFloat()).coerceAtLeast(0f)
              }

              val clamped = sample.coerceIn(-0.85, 0.85)
              pcm[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
            }

            track.write(pcm, 0, pcm.size)
          }
        } catch (_: Throwable) {
          // Ignore audio shutdown race
        } finally {
          runCatching {
            track.stop()
            track.release()
          }
        }
      }
  }

  fun stop() {
    running = false
    audioThread?.interrupt()
    audioThread = null
  }
}
