package com.netsentinel.app.multimedia

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Audio radar synthesizer emitting real audio pings in Hunt Mode.
 * Dynamically adjusts beep pulse interval and pitch based on relative RSSI changes
 * as a directional proximity cue (no exact distance inferred).
 */
class AudioRadarSynthesizer {

    private var toneGenerator: ToneGenerator? = null
    private var handler: Handler? = null
    private var isPlaying = false
    private var currentRssi = -50

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            handler = Handler(Looper.getMainLooper())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startRadar() {
        if (isPlaying) return
        isPlaying = true
        scheduleNextBeep()
    }

    fun stopRadar() {
        isPlaying = false
        handler?.removeCallbacksAndMessages(null)
    }

    fun updateRelativeRssi(rssi: Int) {
        this.currentRssi = rssi.coerceIn(-100, -20)
    }

    private fun scheduleNextBeep() {
        if (!isPlaying) return

        // Calculate relative interval based on RSSI (Stronger signal = faster beeps)
        // RSSI -30dBm -> 150ms interval, RSSI -90dBm -> 1200ms interval
        val normalized = ((currentRssi + 100) / 80f).coerceIn(0f, 1f)
        val intervalMs = (1200 - (normalized * 1050)).toLong()

        try {
            val toneType = if (currentRssi >= -45) ToneGenerator.TONE_CDMA_HIGH_L else ToneGenerator.TONE_PROP_BEEP
            toneGenerator?.startTone(toneType, 80)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        handler?.postDelayed({
            scheduleNextBeep()
        }, intervalMs)
    }

    fun release() {
        stopRadar()
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
