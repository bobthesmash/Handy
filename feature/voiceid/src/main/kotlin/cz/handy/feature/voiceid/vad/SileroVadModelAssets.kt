package cz.handy.feature.voiceid.vad

import android.content.Context
import cz.handy.feature.voiceid.ecapa.EcapaModelAssets

/**
 * ONNX Silero VAD v5 (**stejný asset adresář** jako ECAPA — `voiceid/`).
 *
 * Bez souboru je build platný; inference selže až při běhu ([F1‑T05], ADR‑0003).
 */
object SileroVadModelAssets {
    private const val ONNX_VAD_FILE = "silero_vad.onnx"

    fun relativeOnnxPath(): String = "${EcapaModelAssets.ASSET_DIR}/$ONNX_VAD_FILE"

    fun bundled(context: Context): Boolean {
        val app = context.applicationContext
        val listing =
            runCatching { app.assets.list(EcapaModelAssets.ASSET_DIR) }.getOrNull() ?: return false
        return ONNX_VAD_FILE in listing
    }
}
