package com.example.milo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Feature 2: Hands-Free Voice Mode (MiloVoiceHelper.kt)
 * Integrates Android's native SpeechRecognizer directly into Jetpack Compose so field agents
 * can talk to Milo while driving or visiting client sites.
 * Also provides Milo Text-To-Speech audio briefing on app open!
 */
class MiloVoiceHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onErrorState: (String) -> Unit = {}
) : RecognitionListener {

    companion object {
        private const val TAG = "MiloVoiceHelper"
        private const val PREFS_NAME = "mb_traker_app_prefs"
        private const val KEY_MUTED = "milo_voice_muted"
        private const val KEY_VOICE_OPT = "milo_voice_option"

        private var tts: TextToSpeech? = null
        private var isTtsInitialized = false
        private var pendingTextToSpeak: String? = null

        private val _isMutedState = MutableStateFlow(false)
        val isMutedFlow: StateFlow<Boolean> = _isMutedState.asStateFlow()

        fun init(context: Context) {
            val appContext = context.applicationContext
            val muted = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_MUTED, false)
            _isMutedState.value = muted
            Log.d(TAG, "MiloVoiceHelper initialized with isMuted = $muted")
        }

        fun isMuted(context: Context): Boolean {
            val appContext = context.applicationContext
            val muted = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_MUTED, false)
            _isMutedState.value = muted
            return muted
        }

        fun setMuted(context: Context, muted: Boolean) {
            val appContext = context.applicationContext
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MUTED, muted)
                .apply()
            _isMutedState.value = muted
            Log.d(TAG, "Milo voice mute state updated to: $muted")
            if (muted) {
                stopSpeaking()
                pendingTextToSpeak = null
            }
        }

        fun toggleMute(context: Context): Boolean {
            val currentMuted = isMuted(context)
            val newMuted = !currentMuted
            setMuted(context, newMuted)
            return newMuted
        }

        fun isSpeaking(): Boolean {
            return try {
                tts?.isSpeaking == true
            } catch (_: Exception) {
                false
            }
        }

        fun getVoiceOption(context: Context): String {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_VOICE_OPT, "INDIAN_MALE") ?: "INDIAN_MALE"
        }

        fun setVoiceOption(context: Context, option: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_VOICE_OPT, option)
                .apply()
            // Reset TTS so that on next initialization, the new voice is loaded
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (e: Exception) {
                Log.e(TAG, "Error shutting down TTS during voice switch", e)
            }
            tts = null
            isTtsInitialized = false
        }

        fun initTts(context: Context, onReady: (() -> Unit)? = null) {
            if (tts == null) {
                tts = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        val voiceOpt = getVoiceOption(context)
                        val targetLocale = when (voiceOpt) {
                            "INDIAN_FEMALE", "INDIAN_MALE" -> Locale("en", "IN")
                            "UK_ACCENT" -> Locale.UK
                            "US_ACCENT" -> Locale.US
                            else -> Locale.getDefault()
                        }
                        
                        tts?.language = targetLocale
                        
                        try {
                            val voices = tts?.voices
                            if (!voices.isNullOrEmpty()) {
                                val selectedVoice = when (voiceOpt) {
                                    "INDIAN_MALE" -> {
                                        voices.firstOrNull { voice ->
                                            voice.locale.language == "en" && 
                                            voice.locale.country == "IN" && 
                                            (voice.name.lowercase().contains("male") || voice.name.lowercase().contains("m-") || voice.name.lowercase().contains("ind"))
                                        } ?: voices.firstOrNull { voice ->
                                            voice.locale.language == "en" && voice.locale.country == "IN"
                                        }
                                    }
                                    "INDIAN_FEMALE" -> {
                                        voices.firstOrNull { voice ->
                                            voice.locale.language == "en" && 
                                            voice.locale.country == "IN" && 
                                            (voice.name.lowercase().contains("female") || voice.name.lowercase().contains("f-") || voice.name.lowercase().contains("girl") || voice.name.lowercase().contains("lady"))
                                        } ?: voices.firstOrNull { voice ->
                                            voice.locale.language == "en" && voice.locale.country == "IN"
                                        }
                                    }
                                    "UK_ACCENT" -> {
                                        voices.firstOrNull { voice ->
                                            voice.locale.language == "en" && voice.locale.country == "GB"
                                        }
                                    }
                                    "US_ACCENT" -> {
                                        voices.firstOrNull { voice ->
                                            voice.locale.language == "en" && voice.locale.country == "US"
                                        }
                                    }
                                    else -> null
                                }
                                
                                if (selectedVoice != null) {
                                    tts?.voice = selectedVoice
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error setting voice option $voiceOpt: ${e.message}")
                        }
                        
                        // Set pitch and rate
                        if (voiceOpt == "INDIAN_MALE") {
                            tts?.setPitch(0.98f) // masculine/warm
                        } else {
                            tts?.setPitch(1.0f)
                        }
                        tts?.setSpeechRate(1.02f)
                        isTtsInitialized = true
                        Log.d(TAG, "Milo TextToSpeech ($voiceOpt) initialized successfully")
                        
                        // Speak pending text ONLY IF NOT MUTED
                        if (!isMuted(context)) {
                            pendingTextToSpeak?.let { text ->
                                try {
                                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "milo_speech_${System.currentTimeMillis()}")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error speaking pending text", e)
                                }
                            }
                        }
                        pendingTextToSpeak = null
                        onReady?.invoke()
                    } else {
                        Log.w(TAG, "Milo TextToSpeech failed initialization with status $status")
                        pendingTextToSpeak = null
                    }
                }
            } else if (isTtsInitialized) {
                onReady?.invoke()
            }
        }

        fun speakDailyBriefing(
            context: Context,
            employeeName: String,
            pendingTasksCount: Int,
            activeProjectsCount: Int,
            leadsCount: Int,
            callLogsCount: Int,
            force: Boolean = false
        ) {
            if (isMuted(context)) {
                Log.d(TAG, "Milo voice is muted, skipping daily briefing.")
                stopSpeaking()
                return
            }

            val name = if (employeeName.isNotBlank() && !employeeName.equals("User", true)) employeeName else "Team Member"
            val briefingMessage = buildString {
                append("Good day, $name! ")
                append("Here is your status update from Milo. ")
                if (pendingTasksCount > 0) {
                    append("You have $pendingTasksCount pending tasks requiring attention. ")
                } else {
                    append("All your tasks are currently up to date. ")
                }
                if (activeProjectsCount > 0) {
                    append("There are $activeProjectsCount active projects in progress. ")
                }
                if (leadsCount > 0) {
                    append("You have $leadsCount active leads in the pipeline. ")
                }
                if (callLogsCount > 0) {
                    append("And $callLogsCount call logs are registered. ")
                }
                append("Ready to conquer today's goals!")
            }

            speakText(context, briefingMessage)
        }

        fun speakText(context: Context, text: String) {
            if (isMuted(context)) {
                Log.d(TAG, "Milo voice is muted, skipping speakText: $text")
                stopSpeaking()
                pendingTextToSpeak = null
                return
            }

            if (tts == null || !isTtsInitialized) {
                pendingTextToSpeak = text
                initTts(context)
            } else {
                try {
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "milo_speech_${System.currentTimeMillis()}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error invoking tts.speak", e)
                }
            }
        }

        fun stopSpeaking() {
            pendingTextToSpeak = null
            try {
                tts?.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping TTS", e)
            }
        }
    }

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@MiloVoiceHelper)
            }
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            onErrorState("Speech recognition not available on device")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("MiloVoiceHelper", "Failed to start speech listener", e)
            onErrorState("Error initializing voice: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("MiloVoiceHelper", "Error stopping listener", e)
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("MiloVoiceHelper", "Error destroying speech recognizer", e)
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Speech permissions required"
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timeout"
            else -> "Voice input failed ($error)"
        }
        Log.w("MiloVoiceHelper", "Speech recognizer error: $errorMessage")
        onErrorState(errorMessage)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            Log.d("MiloVoiceHelper", "Recognized voice text: $text")
            onResult(text)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            onResult(matches[0])
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}

/**
 * State object for Jetpack Compose Voice Mode.
 */
data class MiloVoiceState(
    val isListening: Boolean = false,
    val spokenText: String = "",
    val error: String? = null
)

@Composable
fun rememberMiloVoiceState(
    onCommandRecognized: (String) -> Unit
): Pair<MiloVoiceState, () -> Unit> {
    val context = LocalContext.current
    var voiceState by remember { mutableStateOf(MiloVoiceState()) }

    val helper = remember {
        MiloVoiceHelper(
            context = context,
            onResult = { resultText ->
                voiceState = voiceState.copy(
                    isListening = false,
                    spokenText = resultText,
                    error = null
                )
                onCommandRecognized(resultText)
            },
            onErrorState = { err ->
                voiceState = voiceState.copy(
                    isListening = false,
                    error = err
                )
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            helper.destroy()
        }
    }

    val toggleListening = {
        if (voiceState.isListening) {
            helper.stopListening()
            voiceState = voiceState.copy(isListening = false)
        } else {
            voiceState = voiceState.copy(isListening = true, error = null)
            helper.startListening()
        }
    }

    return Pair(voiceState, toggleListening)
}
