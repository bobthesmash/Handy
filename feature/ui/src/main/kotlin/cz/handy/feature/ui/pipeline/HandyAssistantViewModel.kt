package cz.handy.feature.ui.pipeline

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.handy.core.audio.EarAudioBridge
import cz.handy.core.common.audio.AsrHypothesisConfidence
import cz.handy.core.common.dialog.DialogManager
import cz.handy.core.persistence.HandyLocalTelemetry
import cz.handy.core.persistence.LocalTelemetryPreferences
import cz.handy.core.persistence.PipelineLatencyTracer
import cz.handy.feature.actions.executor.MvpIntentExecutor
import cz.handy.feature.asr.SherpaStreamingRecognizerHolder
import cz.handy.feature.nlu.HandyNluCatalogs
import cz.handy.feature.nlu.LlmPrimaryRuleFallbackNluEngine
import cz.handy.feature.nlu.NluResult
import cz.handy.feature.nlu.ParsedIntent
import cz.handy.feature.nlu.RuleBasedNluEngine
import cz.handy.feature.nlu.UnbundledLlmNluParser
import cz.handy.feature.tts.AndroidCzechSpeechSynthesizer
import cz.handy.feature.tts.SpeechSynthesizer
import cz.handy.feature.ui.R
import cz.handy.feature.voiceid.antispoof.AntiSpoofInferenceException
import cz.handy.feature.voiceid.antispoof.AntiSpoofRejectedException
import cz.handy.feature.voiceid.confirm.DestructiveConfirmVoiceVerifier
import cz.handy.feature.voiceid.ecapa.SpeechbrainEcapaPreprocessor
import cz.handy.feature.voiceid.storage.SpeakerEmbeddingEncryptedStore
import cz.handy.feature.voiceid.verify.VerificationVerdict
import cz.handy.feature.wakeword.WakeWordSignalBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Pohon pipeline **Idle→…→NLU→(Confirm)→Exec**; PCM z [EarService] přes [EarAudioBridge]
 * + Sherpa po [noteWakeWordForHeavyModels], textový panel slouží i k ručním testům ([F1-T06]/[F1-T07]).
 *
 * @param simulateVoicePipelineBypass Když `true` (jen debug — `MainActivity` jej páruje s `showCommandPipelineUi` =
 * `BuildConfig.DEBUG`): příkaz projde bez uloženého centroidu majitele a přeskakuje wake/verify fáze v dialogu.
 * **Release má vždy `false`.** Pokud je `false` a chybí profil, vstup před NLU se odmítne.
 */
@Suppress("TooManyFunctions")
class HandyAssistantViewModel(
    application: Application,
    private val simulateVoicePipelineBypass: Boolean,
) : AndroidViewModel(application) {
    private val embeddingStore = SpeakerEmbeddingEncryptedStore(application)

    private val dialog = DialogManager()
    private val nluEngine =
        LlmPrimaryRuleFallbackNluEngine(
            llm = UnbundledLlmNluParser,
            rules = RuleBasedNluEngine(HandyNluCatalogs.mvp),
        )
    private val executor = MvpIntentExecutor(application)
    private val speech: SpeechSynthesizer = AndroidCzechSpeechSynthesizer(application)
    private val destructiveVoiceConfirm = DestructiveConfirmVoiceVerifier(application)
    private val sherpaHolder = SherpaStreamingRecognizerHolder(application)
    private val telemetry =
        HandyLocalTelemetry(application, LocalTelemetryPreferences(application))

    /** Po [HEAVY_MODEL_IDLE_MINUTES] bez interakce uvolníme ONNX ECAPA session a Sherpa graf ([F3-T05]). */
    private var heavyModelsIdleJob: Job? = null

    /** Čte [EarAudioBridge] a krmí [SherpaStreamingRecognizerHolder] po [noteWakeWordForHeavyModels]. */
    private var micFeedJob: Job? = null

    private var pcmConsumeMarker: Long = 0L

    /** PCM tahu ze Sherpy před NLU — jen při `!simulateVoicePipelineBypass` a uloženém profilu. */
    private val phraseTurnPcmChunks = ArrayList<ShortArray>()
    private var capturePhraseTurnPcm = false

    private val micFeedAllowed = AtomicBoolean(false)

    /** Začátek tahu (pro lokální telemetrii latence [F2-T13]). */
    private var pendingTurnStartElapsed: Long? = null

    private val _voiceConfirmBusy = MutableStateFlow(false)
    val voiceConfirmBusy: StateFlow<Boolean> = _voiceConfirmBusy.asStateFlow()

    private val _pendingDestructive =
        MutableStateFlow<ParsedIntent?>(null)
    val pendingDestructive: StateFlow<ParsedIntent?> =
        _pendingDestructive.asStateFlow()

    private val _toastLine = MutableStateFlow<String?>(null)

    /** Poslední text předaný do TTS (pro intent REPEAT). */
    private var lastSpokenLine: String? = null

    /** Poslední hláška asistenta (zároveň titulek v demo panelu; čtení přes [speech]). */
    val toastLine: StateFlow<String?> = _toastLine.asStateFlow()

    val dialogPhase get() = dialog.phase

    init {
        viewModelScope.launch(Dispatchers.Default) {
            WakeWordSignalBus.wakes.collect {
                noteWakeWordForHeavyModels()
            }
        }
    }

    class Factory(
        private val application: Application,
        private val simulateVoicePipelineBypass: Boolean,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(HandyAssistantViewModel::class.java))
            return HandyAssistantViewModel(
                application,
                simulateVoicePipelineBypass,
            ) as T
        }
    }

    /**
     * Textové pole simuluje výstup ASR před NLU ([F1-T06]/[F1-T07]).
     * Při napojení Sherpy předej [minTokenProb] z posledního `ysProbs` (nejmenší hodnota).
     */
    fun submitRecognizedPhrase(
        text: String,
        minTokenProb: Float? = null,
        utterancePcmMono16Le: ShortArray? = null,
    ) {
        viewModelScope.launch {
            cancelHeavyModelsIdleRelease()
            try {
                handleSimulatedTranscript(text.trim(), minTokenProb, utterancePcmMono16Le)
            } finally {
                scheduleHeavyModelsIdleRelease()
            }
        }
    }

    /** Stejné kanály jako [toastLine] — krátké hlášení z UI (např. nepovedený start nahrávání). */
    fun notifyAssistantLine(message: String) {
        _toastLine.value = message
    }

    /** Druhý speaker-verify (≥ T_high) před `SEND_SMS` / `CALL` / `SET_ALARM` / `REPLY_NOTIF` ([F1-T16], [F2-T02]). */
    fun submitDestructiveVoiceConfirmFromPcm(pcmMono16Le: ShortArray) {
        val pending = _pendingDestructive.value ?: return
        if (_voiceConfirmBusy.value) return
        cancelHeavyModelsIdleRelease()
        _voiceConfirmBusy.value = true
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val verified = destructiveVoiceConfirm.verifyDestructiveConfirmUtterance(pcmMono16Le)
                withContext(Dispatchers.Main.immediate) {
                    if (verified.isSuccess) {
                        _pendingDestructive.value = null
                        dialog.onUserConfirmed()
                        dispatchExec(pending, destructiveSmsConfirmed = true)
                    } else {
                        pendingTurnStartElapsed = null
                        val ex = verified.exceptionOrNull()
                        when (ex) {
                            is AntiSpoofRejectedException -> telemetry.recordAntiSpoofGate("reject")
                            is AntiSpoofInferenceException -> telemetry.recordAntiSpoofGate("onnx_error")
                            else -> {}
                        }
                        val app = getApplication<Application>()
                        _toastLine.value =
                            when (ex) {
                                is AntiSpoofRejectedException ->
                                    app.getString(R.string.assistant_voice_gate_anti_spoof)
                                is AntiSpoofInferenceException ->
                                    app.getString(R.string.assistant_voice_gate_error)
                                else -> ex?.message ?: "Ověření hlasu selhalo."
                            }
                    }
                }            } finally {
                withContext(Dispatchers.Main.immediate) {
                    _voiceConfirmBusy.value = false
                }
                scheduleHeavyModelsIdleRelease()
            }
        }
    }

    /**
     * Zavolá pipeline po wake-wordu: přednačte Sherpu (lazy), ECAPA zůstává lazy až do verify ([F3-T05]).
     * Po [HEAVY_MODEL_IDLE_MINUTES] bez další aktivity se uvolní paměť.
     */
    fun noteWakeWordForHeavyModels() {
        viewModelScope.launch(Dispatchers.Default) {
            cancelHeavyModelsIdleRelease()
            PipelineLatencyTracer.markWakeWordSignal()
            val rec = sherpaHolder.acquire()
            PipelineLatencyTracer.markSherpaRecognizerReady(rec != null)
            if (rec != null) {
                startMicFeedToSherpa()
            } else {
                stopMicFeed()
            }
            scheduleHeavyModelsIdleRelease()
        }
    }

    fun rejectPendingDestructive() {
        if (_pendingDestructive.value == null) return
        viewModelScope.launch {
            dialog.onUserRejected()
            _pendingDestructive.value = null
            pendingTurnStartElapsed = null
            val line = "Akce zrušena."
            lastSpokenLine = line
            _toastLine.value = line
            speech.speak(line) { }
        }
    }

    override fun onCleared() {
        cancelHeavyModelsIdleRelease()
        releaseHeavyInferenceSessions()
        speech.shutdown()
        super.onCleared()
    }

    private fun cancelHeavyModelsIdleRelease() {
        heavyModelsIdleJob?.cancel()
        heavyModelsIdleJob = null
    }

    private fun scheduleHeavyModelsIdleRelease() {
        cancelHeavyModelsIdleRelease()
        heavyModelsIdleJob =
            viewModelScope.launch {
                delay(TimeUnit.MINUTES.toMillis(HEAVY_MODEL_IDLE_MINUTES))
                releaseHeavyInferenceSessions()
            }
    }

    private fun releaseHeavyInferenceSessions() {
        stopMicFeed()
        destructiveVoiceConfirm.releaseOnnxResources()
        sherpaHolder.release()
    }

    private fun stopMicFeed() {
        micFeedAllowed.set(false)
        micFeedJob?.cancel()
        micFeedJob = null
        capturePhraseTurnPcm = false
        phraseTurnPcmChunks.clear()
    }

    private fun startMicFeedToSherpa() {
        stopMicFeed()
        val rec = sherpaHolder.peek() ?: return
        runCatching { rec.startUtterance() }
            .onFailure {
                return
            }
        capturePhraseTurnPcm =
            !simulateVoicePipelineBypass &&
            embeddingStore.hasSpeakerProfile()
        phraseTurnPcmChunks.clear()
        val ring = EarAudioBridge.ringBufferOrNull() ?: return
        pcmConsumeMarker = ring.totalSamplesWritten()
        micFeedAllowed.set(true)
        micFeedJob =
            viewModelScope.launch(Dispatchers.Default) {
                while (isActive && micFeedAllowed.get()) {
                    if (!micFeedAllowed.get()) {
                        break
                    }
                    val r = sherpaHolder.peek() ?: break
                    val buffer = EarAudioBridge.ringBufferOrNull() ?: break
                    val (chunk, newMark) = buffer.consumeMono16SinceTotalWritten(pcmConsumeMarker)
                    pcmConsumeMarker = newMark
                    if (chunk.isNotEmpty() && micFeedAllowed.get()) {
                        appendPhraseTurnChunk(chunk)
                        val tick = r.appendPcm16Mono(chunk)
                        if (tick.text.isNotBlank()) {
                            PipelineLatencyTracer.markFirstAsrPartial(true)
                        }
                        if (tick.endpoint && tick.text.isNotBlank()) {
                            val pcmSnapshot =
                                if (capturePhraseTurnPcm) {
                                    drainPhraseTurnPcm()
                                } else {
                                    null
                                }
                            capturePhraseTurnPcm = false
                            withContext(Dispatchers.Main.immediate) {
                                submitRecognizedPhrase(
                                    tick.text,
                                    tick.minTokenProb,
                                    pcmSnapshot,
                                )
                            }
                        }
                    }
                    delay(MIC_FEED_POLL_MS)
                }
            }
    }

    fun consumeToast() {
        _toastLine.value = null
    }

    private fun appendPhraseTurnChunk(chunk: ShortArray) {
        if (!capturePhraseTurnPcm || chunk.isEmpty()) return
        phraseTurnPcmChunks.add(chunk.copyOf())
    }

    private fun drainPhraseTurnPcm(): ShortArray {
        var len = 0
        for (c in phraseTurnPcmChunks) {
            len += c.size
        }
        val out = ShortArray(len)
        var offset = 0
        for (c in phraseTurnPcmChunks) {
            System.arraycopy(c, 0, out, offset, c.size)
            offset += c.size
        }
        phraseTurnPcmChunks.clear()
        return out
    }

    private suspend fun handleSimulatedTranscript(
        trimmed: String,
        minTokenProb: Float? = null,
        utterancePcmMono16Le: ShortArray? = null,
    ) {
        if (trimmed.isBlank()) {
            _toastLine.value = "Prázdný text."
            return
        }
        if (AsrHypothesisConfidence.shouldAskRepeat(trimmed, minTokenProb)) {
            speech.stop()
            dialog.resetToIdle()
            _pendingDestructive.value = null
            pendingTurnStartElapsed = null
            telemetry.recordLowConfidenceAsrRetry()
            val line = "Neslyšel jsem, opakuj."
            lastSpokenLine = line
            _toastLine.value = line
            speech.speak(line) { }
            return
        }
        pendingTurnStartElapsed = SystemClock.elapsedRealtime()

        if (!simulateVoicePipelineBypass && !embeddingStore.hasSpeakerProfile()) {
            pendingTurnStartElapsed = null
            speech.stop()
            dialog.resetToIdle()
            val line =
                getApplication<Application>().getString(
                    R.string.assistant_blocked_no_voice_profile,
                )
            lastSpokenLine = line
            _toastLine.value = line
            speech.speak(line) { }
            return
        }

        runCatching {
            speech.stop()
            dialog.resetToIdle()
            _pendingDestructive.value = null

            val runOwnerPhraseGate =
                !simulateVoicePipelineBypass &&
                    embeddingStore.hasSpeakerProfile() &&
                    utterancePcmMono16Le != null

            if (runOwnerPhraseGate) {
                advancePhraseTurnThroughVerifyGateEntry()
                if (!completeSpeakerPhraseGateOrAbort(utterancePcmMono16Le!!)) {
                    return@runCatching
                }
                advancePhraseTurnAfterSpeakerAccepted()
            } else {
                advanceWakeVerifyStagesForBypassDemoPipeline()
            }

            when (val out = nluEngine.parse(trimmed)) {
                NluResult.NoMatch -> {
                    dialog.abortToIdle()
                    pendingTurnStartElapsed = null
                    _toastLine.value = "NLU: nerozumím."
                }

                is NluResult.Matched -> {
                    when (out.intent.intentId) {
                        "CANCEL" -> finishMetaAssistantLine("Zrušeno.")
                        "STOP" -> finishMetaAssistantLine("Zastaveno.")
                        "REPEAT" -> finishMetaAssistantLine(repeatLineOrFallback())
                        else -> {
                            dialog.onNluComplete(out.intent.requiresConfirm)
                            if (out.intent.requiresConfirm) {
                                _pendingDestructive.value = out.intent
                            } else {
                                dispatchExec(out.intent, destructiveSmsConfirmed = true)
                            }
                        }
                    }
                }
            }
        }.onFailure { err ->
            speech.stop()
            dialog.abortToIdle()
            pendingTurnStartElapsed = null
            _toastLine.value = err.message ?: "Chyba dialogu."
        }
    }

    /**
     * Volitelný anti-spoof (`anti_spoof.onnx`) a pak ECAPA kosínová brána před NLU na PCM ze Sherpa tahu
     * ([DualThresholdSpeakerVerifier]).
     * [VerificationVerdict.Uncertain]: stejně jako Reject z pohledu pipeline — žádný NLU; TTS žádá nový pokus (slabý signál).
     * @return `false`, pokud řetězec končí hláškou uživateli (bez NLU).
     */
    private suspend fun completeSpeakerPhraseGateOrAbort(pcmMono16Le: ShortArray): Boolean {
        val app = getApplication<Application>()
        if (pcmMono16Le.size < SpeechbrainEcapaPreprocessor.MIN_PCM_SAMPLES) {
            telemetry.recordSpeakerPhraseGate("short_pcm")
            pendingTurnStartElapsed = null
            speech.stop()
            dialog.onVerifyFailed()
            val line = app.getString(R.string.assistant_voice_gate_audio_short)
            lastSpokenLine = line
            _toastLine.value = line
            speech.speak(line) { }
            return false
        }

        val verdictResult =
            withContext(Dispatchers.Default) {
                destructiveVoiceConfirm.evaluateTurnAgainstStoredProfile(pcmMono16Le)
            }

        if (verdictResult.isFailure) {
            val ex = verdictResult.exceptionOrNull()
            when (ex) {
                is AntiSpoofRejectedException -> {
                    telemetry.recordAntiSpoofGate("reject")
                    telemetry.recordSpeakerPhraseGate("blocked_anti_spoof")
                }

                is AntiSpoofInferenceException -> {
                    telemetry.recordAntiSpoofGate("onnx_error")
                    telemetry.recordSpeakerPhraseGate("anti_spoof_onnx_error")
                }

                else -> telemetry.recordSpeakerPhraseGate("onnx_error")
            }
            pendingTurnStartElapsed = null
            speech.stop()
            dialog.abortToIdle()
            val line =
                when (ex) {
                    is AntiSpoofRejectedException -> app.getString(R.string.assistant_voice_gate_anti_spoof)
                    is AntiSpoofInferenceException -> app.getString(R.string.assistant_voice_gate_error)
                    else ->
                        ex?.message?.takeIf { it.isNotBlank() }
                            ?: app.getString(R.string.assistant_voice_gate_error)
                }
            lastSpokenLine = line
            _toastLine.value = line
            speech.speak(line) { }
            return false
        }
        return when (verdictResult.getOrThrow()) {
            VerificationVerdict.StrongAccept -> true

            VerificationVerdict.Uncertain -> {
                telemetry.recordSpeakerPhraseGate("uncertain")
                pendingTurnStartElapsed = null
                speech.stop()
                dialog.onVerifyFailed()
                val line = app.getString(R.string.assistant_voice_uncertain_repeat)
                lastSpokenLine = line
                _toastLine.value = line
                speech.speak(line) { }
                false
            }

            VerificationVerdict.Reject -> {
                telemetry.recordSpeakerPhraseGate("reject")
                pendingTurnStartElapsed = null
                speech.stop()
                dialog.onVerifyFailed()
                val line = app.getString(R.string.assistant_voice_rejected)
                lastSpokenLine = line
                _toastLine.value = line
                speech.speak(line) { }
                false
            }
        }
    }

    /** Demo pipeline bez měření Verify na PCM — textové pole / bypass profilu. */
    private fun advanceWakeVerifyStagesForBypassDemoPipeline() {
        dialog.onWakeDetected()
        dialog.onVerifyStarted()
        dialog.onVerifyPassed()
        dialog.onCaptureComplete()
        dialog.onAsrComplete()
    }

    private fun advancePhraseTurnThroughVerifyGateEntry() {
        dialog.onWakeDetected()
        dialog.onVerifyStarted()
    }

    private fun advancePhraseTurnAfterSpeakerAccepted() {
        dialog.onVerifyPassed()
        dialog.onCaptureComplete()
        dialog.onAsrComplete()
    }

    private fun dispatchExec(
        intent: ParsedIntent,
        destructiveSmsConfirmed: Boolean,
    ) {
        val smsFlag =
            if (intent.intentId == "SEND_SMS") {
                destructiveSmsConfirmed
            } else {
                false
            }
        val ack =
            executor.execute(intent, smsExplicitConfirm = smsFlag).getOrElse { err ->
                speech.stop()
                dialog.abortToIdle()
                pendingTurnStartElapsed = null
                val base = err.message?.takeIf { it.isNotBlank() } ?: err::class.simpleName.orEmpty()
                val extra =
                    err.suppressedExceptions.mapNotNull { it.message }.filter { it.isNotBlank() }
                _toastLine.value =
                    (
                        sequenceOf(base) + extra.asSequence()
                    ).joinToString(" — ")
                        .ifBlank { err.toString() }
                return
            }
        pendingTurnStartElapsed?.let { t0 ->
            telemetry.recordIntentCompleted(
                intentId = intent.intentId,
                latencySinceTurnStartMs = SystemClock.elapsedRealtime() - t0,
            )
        }
        pendingTurnStartElapsed = null
        dialog.onExecComplete()
        lastSpokenLine = ack
        _toastLine.value = ack
        speech.speak(ack) { dialog.onTtsComplete() }
    }

    private fun repeatLineOrFallback(): String = lastSpokenLine?.takeIf { it.isNotBlank() } ?: "Nemám co opakovat."

    /** Zastaví probíhající TTS / zruší čekání; krátké potvrzení bez práce s [DialogPhase.Exec]. */
    private fun finishMetaAssistantLine(line: String) {
        dialog.resetToIdle()
        pendingTurnStartElapsed = null
        lastSpokenLine = line
        _toastLine.value = line
        speech.speak(line) { }
    }

    private companion object {
        private const val HEAVY_MODEL_IDLE_MINUTES = 5L
        private const val MIC_FEED_POLL_MS = 25L
    }
}
