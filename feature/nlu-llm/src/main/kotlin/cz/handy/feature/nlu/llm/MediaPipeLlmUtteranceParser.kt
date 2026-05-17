package cz.handy.feature.nlu.llm

import android.app.Application
import cz.handy.feature.nlu.IntentCatalog
import cz.handy.feature.nlu.LlmStructuredResponseParser
import cz.handy.feature.nlu.NluResult
import cz.handy.feature.nlu.UtteranceNluParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lokální Gemma / kompatibilní `.task` model přes MediaPipe LLM Inference ([F5-T01]).
 * Bez souboru `assets/nlu_llm/gemma_hand_task.task` vrací vždy [NluResult.NoMatch] — projdou pravidla.
 */
class MediaPipeLlmUtteranceParser(
    private val application: Application,
    private val catalog: IntentCatalog,
) : UtteranceNluParser {
    private val mutex = Mutex()
    private val runtime: MediaPipeLlmRuntime? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        resolveBundledModelPath(application)?.let { MediaPipeLlmRuntime.createIfPossible(application, it) }
    }

    override suspend fun parse(utterance: String): NluResult {
        if (utterance.isBlank()) return NluResult.NoMatch

        val direct = LlmStructuredResponseParser.parseFromRaw(utterance, catalog)
        if (direct !is NluResult.NoMatch) return direct

        val rt = runtime ?: return NluResult.NoMatch
        val prompt = buildPrompt(utterance)

        return withContext(Dispatchers.Default) {
            mutex.withLock {
                try {
                    val raw = rt.generate(prompt)
                    LlmStructuredResponseParser.parseFromRaw(raw, catalog)
                } catch (_: Throwable) {
                    NluResult.NoMatch
                }
            }
        }
    }

    private fun buildPrompt(utterance: String): String {
        val ids = catalog.intentIds.joinToString(", ")
        return listOf(
            "Reply with ONE compact JSON object only (no markdown fences).",
            """Schema: {"intentId":"ID","slots":{"slot":"value"},"requiresConfirm":optional_bool}""",
            "Allowed intentId values: $ids",
            "Slots must satisfy non-empty required slots for that intent; use {} if none.",
            "ASR text (Czech or English): ${utterance.trim()}",
        ).joinToString("\n")
    }

    companion object {
        private const val TASK_ASSET_PATH = "nlu_llm/gemma_hand_task.task"

        fun create(
            application: Application,
            catalog: IntentCatalog,
        ): UtteranceNluParser = MediaPipeLlmUtteranceParser(application, catalog)

        internal fun resolveBundledModelPath(app: Application): String? {
            val dir = File(app.filesDir, "nlu_llm")
            val target = File(dir, "gemma_hand_task.task")
            if (target.isFile) return target.absolutePath
            return try {
                dir.mkdirs()
                app.assets.open(TASK_ASSET_PATH).use { input ->
                    target.outputStream().use { out -> input.copyTo(out) }
                }
                target.takeIf { it.isFile }?.absolutePath
            } catch (_: Exception) {
                null
            }
        }
    }
}
