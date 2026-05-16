package cz.handy.feature.wakeword

import android.content.Context
import com.rementia.openwakeword.lib.WakeWordEngine
import com.rementia.openwakeword.lib.model.DetectionMode
import com.rementia.openwakeword.lib.model.WakeWordModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Thin wrapper over `xyz.rementia:openwakeword` ([com.rementia.openwakeword.lib.WakeWordEngine]).
 *
 * Models must live under [ASSET_DIR] in the merged APK assets (typically provided by this module or `:app`).
 *
 * **Mic ownership:** [WakeWordEngine.start] pulls PCM through the library's internal [android.media.AudioRecord]
 * (`AudioRecorder` in upstream sources). There is no stable public API to inject the same mono stream as
 * [cz.handy.core.audio.EarService] / [cz.handy.core.audio.EarAudioBridge]; see ADR `0001-wake-word.md`. Only call
 * [WakeWordEngine.start] after [RECORD_AUDIO] is granted, and avoid running it alongside production Porcupine wake
 * unless you accept a **second** recorder.
 */
object OpenWakeWordEngineFactory {
    /** Audio window length passed to [WakeWordEngine] (library default semantics). */
    private const val ENGINE_AUDIO_WINDOW_MS = 2000L

    const val ASSET_DIR = "openwakeword"
    const val FILE_MEL = "melspectrogram.onnx"
    const val FILE_EMBED = "embedding_model.onnx"
    const val FILE_KEYWORD = "hey_handy.onnx"

    val REQUIRED_ASSETS: List<String> = listOf(FILE_MEL, FILE_EMBED, FILE_KEYWORD)

    fun hasBundledModels(context: Context): Boolean {
        val names = context.assets.list(ASSET_DIR)?.toSet() ?: return false
        return REQUIRED_ASSETS.all { names.contains(it) }
    }

    /**
     * @param wakeWordAssetFile classifier ONNX under [ASSET_DIR] (default [FILE_KEYWORD] placeholder name).
     */
    fun createEngine(
        context: Context,
        wakeWordAssetFile: String = FILE_KEYWORD,
        threshold: Float = 0.12f,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    ): WakeWordEngine? {
        val app = context.applicationContext
        if (!hasBundledModels(app)) return null

        val models =
            listOf(
                WakeWordModel(
                    name = "default",
                    modelPath = "$ASSET_DIR/$wakeWordAssetFile",
                    threshold = threshold,
                ),
            )
        return WakeWordEngine(
            app,
            models,
            DetectionMode.SINGLE_BEST,
            ENGINE_AUDIO_WINDOW_MS,
            scope,
        )
    }
}
