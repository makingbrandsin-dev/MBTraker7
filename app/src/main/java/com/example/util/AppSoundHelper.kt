package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * 🎵 Sound Preset Definitions for Admin Customizable Alerts
 */
enum class AppSoundType(val displayName: String, val tones: List<Pair<Int, Int>>) {
    BEEP_ACK("Tritone Ack (Task standard)", listOf(ToneGenerator.TONE_PROP_BEEP to 80, ToneGenerator.TONE_PROP_ACK to 140)),
    DOUBLE_CHIRP("Double Chirp (Success / Done)", listOf(ToneGenerator.TONE_PROP_BEEP to 80, ToneGenerator.TONE_PROP_BEEP2 to 120, ToneGenerator.TONE_PROP_ACK to 220)),
    KEYPAD_VOLUME("Keypad Volume (Lead standard)", listOf(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE to 90, ToneGenerator.TONE_PROP_ACK to 160)),
    CRISP_PING("Crisp Ping (Chat standard)", listOf(ToneGenerator.TONE_PROP_BEEP to 70)),
    CYBER_SWOOSH("Cyber Swoosh (Urgent / System)", listOf(ToneGenerator.TONE_CDMA_ANSWER to 150)),
    CHIRP_CONFIRM("Chirp Confirm (Punch-In)", listOf(ToneGenerator.TONE_CDMA_CONFIRM to 100)),
    HIGH_CHIME("High Chime (Bright Ding)", listOf(ToneGenerator.TONE_SUP_DIAL to 180)),
    PIP_ALERT("Double Pip Alert", listOf(ToneGenerator.TONE_SUP_PIP to 80, ToneGenerator.TONE_SUP_PIP to 80)),
    SYSTEM_ALERT("Default System Notification", emptyList())
}

/**
 * High-Security Acoustic Feedback Manager
 * Allows dynamic sound presets to be associated with different notification types from the Admin panel.
 */
object AppSoundHelper {

    private const val TAG = "AppSoundHelper"
    private val mainHandler = Handler(Looper.getMainLooper())

    fun playTaskCompletedSound(context: Context) {
        playCategorySound(context, "task")
    }

    fun playProjectDoneSound(context: Context) {
        playPresetSound(context, "DOUBLE_CHIRP")
    }

    fun playLeadAddedSound(context: Context) {
        playCategorySound(context, "lead")
    }

    fun playChatNotificationSound(context: Context) {
        playCategorySound(context, "chat")
    }

    fun playGeneralNotificationSound(context: Context) {
        playCategorySound(context, "system")
    }

    /**
     * Resolves the selected sound preset for a given category and plays it instantly.
     */
    fun playCategorySound(context: Context, category: String) {
        val presetName = AppPreferences.getNotificationSoundPreset(context, category)
        playPresetSound(context, presetName)
    }

    /**
     * Plays a specific preset by name.
     */
    fun playPresetSound(context: Context, presetName: String) {
        if (!AppPreferences.isNotificationSoundEnabled(context)) return
        val preset = try {
            AppSoundType.valueOf(presetName.trim().uppercase())
        } catch (e: Exception) {
            AppSoundType.SYSTEM_ALERT
        }

        if (preset == AppSoundType.SYSTEM_ALERT || preset.tones.isEmpty()) {
            playSystemNotificationSound(context)
        } else {
            playToneSequence(preset.tones, RingtoneManager.TYPE_NOTIFICATION, context)
        }
    }

    private fun playToneSequence(
        tones: List<Pair<Int, Int>>,
        fallbackType: Int,
        context: Context
    ) {
        Thread {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                for (tonePair in tones) {
                    toneGen.startTone(tonePair.first, tonePair.second)
                    Thread.sleep(tonePair.second.toLong() + 40L)
                }
                toneGen.release()
            } catch (e: Exception) {
                Log.d(TAG, "ToneGenerator unavailable, playing default ringtone: ${e.message}")
                mainHandler.post {
                    playSystemNotificationSound(context, fallbackType)
                }
            }
        }.start()
    }

    private fun playSystemNotificationSound(context: Context, type: Int = RingtoneManager.TYPE_NOTIFICATION) {
        try {
            val uri = RingtoneManager.getDefaultUri(type)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play ringtone sound", e)
        }
    }
}
