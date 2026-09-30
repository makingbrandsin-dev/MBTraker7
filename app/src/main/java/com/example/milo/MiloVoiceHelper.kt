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
        private var tts: TextToSpeech? = null
        private var isTtsInitialized = false
        private var pendingTextToSpeak: String? = null

        fun initTts(context: Context, onReady: (() -> Unit)? = null) {
            if (tts == null) {
                tts = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        tts?.language = Locale.ENGLISH
                        tts?.setPitch(1.08f)
                        tts?.setSpeechRate(1.02f)
                        isTtsInitialized = true
                        Log.d(TAG, "Milo TextToSpeech initialized successfully")
                        pendingTextToSpeak?.let { text ->
                            speakText(context, text)
                            pendingTextToSpeak = null
                        }
                        onReady?.invoke()
                    } else {
                        Log.w(TAG, "Milo TextToSpeech failed initialization with status $status")
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
            if (tts == null || !isTtsInitialized) {
                pendingTextToSpeak = text
                initTts(context) {
                    try {
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "milo_speech_${System.currentTimeMillis()}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error speaking text", e)
                    }
                }
            } else {
                try {
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "milo_speech_${System.currentTimeMillis()}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error invoking tts.speak", e)
                }
            }
        }

        fun stopSpeaking() {
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
