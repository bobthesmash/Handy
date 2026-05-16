package cz.handy.feature.voiceid.antispoof

import android.content.Context

/**
 * Volitelný binární klasifikátor replay/TTS (ONNX).
 *
 * Stejný asset adresář jako ECAPA (`voiceid/`). Bez souboru se brána v kódu přeskočí — ECAPA zůstává
 * aktivní (`docs/decisions/0007-anti-spoofing-onnx.md`).
 */
object AntiSpoofModelAssets {
    private const val ASSET_DIR = "voiceid"

    /** Soubor ONNX v `feature/voiceid/src/main/assets/voiceid/`. */
    const val ONNX_FILE = "anti_spoof.onnx"

    fun relativeOnnxPath(): String = "$ASSET_DIR/$ONNX_FILE"

    fun bundled(context: Context): Boolean {
        val app = context.applicationContext
        val listing = runCatching { app.assets.list(ASSET_DIR) }.getOrNull() ?: return false
        return ONNX_FILE in listing
    }
}
