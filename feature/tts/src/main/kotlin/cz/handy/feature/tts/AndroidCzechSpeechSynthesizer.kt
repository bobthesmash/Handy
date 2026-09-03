package cz.handy.feature.tts

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * [TextToSpeech] s preferencí `cs-CZ`; při chybě jazyka použije výchozí hlas zařízení.
 *
 * Veřejné metody musí volat hlavní vlákno.
 */
class AndroidCzechSpeechSynthesizer(
    context: Context,
) : SpeechSynthesizer {
    private val app = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val gate = UtteranceSequenceGate()

    @Volatile
    private var tts: TextToSpeech? = null

    @Volatile
    private var initInFlight = false

    private data class PendingSpeak(
        val text: String,
        val utteranceId: Int,
        val onComplete: () -> Unit,
    )

    @Volatile
    private var pendingWhileInit: PendingSpeak? = null

    override fun stop() {
        assertMainThread()
        gate.bumpOnStop()
        pendingWhileInit = null
        tts?.stop()
    }

    override fun speak(
        text: String,
        onComplete: () -> Unit,
    ) {
        assertMainThread()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            onComplete()
            return
        }
        val utteranceId = gate.nextUtteranceId()
        tts?.stop()

        val engine = tts
        if (engine != null) {
            pendingWhileInit = null
            speakOnEngine(engine, trimmed, utteranceId, onComplete)
            return
        }

        pendingWhileInit = PendingSpeak(trimmed, utteranceId, onComplete)

        if (initInFlight) {
            return
        }
        initInFlight = true
        tts =
            TextToSpeech(app) { status ->
                mainHandler.post {
                    initInFlight = false
                    val ready = tts
                    val pending = pendingWhileInit
                    when {
                        ready == null -> {
                            pending?.let { p ->
                                if (gate.isCurrentUtterance(p.utteranceId)) {
                                    p.onComplete()
                                }
                            }
                            pendingWhileInit = null
                        }

                        status != TextToSpeech.SUCCESS -> {
                            ready.shutdown()
                            if (tts === ready) {
                                tts = null
                            }
                            pendingWhileInit = null
                            pending?.let { p ->
                                if (gate.isCurrentUtterance(p.utteranceId)) {
                                    p.onComplete()
                                }
                            }
                        }

                        pending == null || !gate.isCurrentUtterance(pending.utteranceId) -> {
                            pendingWhileInit = null
                        }

                        else -> {
                            applyPreferredLanguage(ready)
                            pendingWhileInit = null
                            speakOnEngine(ready, pending.text, pending.utteranceId, pending.onComplete)
                        }
                    }
                }
            }
    }

    override fun shutdown() {
        assertMainThread()
        gate.bumpOnStop()
        pendingWhileInit = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun speakOnEngine(
        engine: TextToSpeech,
        text: String,
        utteranceId: Int,
        onComplete: () -> Unit,
    ) {
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(rawId: String?) {
                    val parsed = rawId.parseGateId()
                    val id =
                        if (parsed >= 0) {
                            parsed
                        } else {
                            utteranceId
                        }
                    if (gate.isCurrentUtterance(id)) {
                        mainHandler.post { onComplete() }
                    }
                }

                @Deprecated("Deprecated in Java")
                @Suppress("DEPRECATION")
                override fun onError(utteranceId: String?) {
                    onDone(utteranceId)
                }

                override fun onError(
                    utteranceId: String?,
                    errorCode: Int,
                ) {
                    onDone(utteranceId)
                }
            },
        )
        val params =
            Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId.toString())
            }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId.toString())
    }

    private fun applyPreferredLanguage(engine: TextToSpeech) {
        val en = Locale.US
        when (engine.setLanguage(en)) {
            TextToSpeech.LANG_MISSING_DATA,
            TextToSpeech.LANG_NOT_SUPPORTED,
            -> engine.setLanguage(Locale.getDefault())
            else -> Unit
        }
    }

    private fun assertMainThread() {
        check(Looper.getMainLooper() == Looper.myLooper()) {
            "AndroidCzechSpeechSynthesizer musí být volán z hlavního vlákna."
        }
    }

    private fun String?.parseGateId(): Int = this?.toIntOrNull() ?: -1
}
