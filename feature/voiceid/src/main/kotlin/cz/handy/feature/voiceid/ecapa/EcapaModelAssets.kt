package cz.handy.feature.voiceid.ecapa

import android.content.Context

/**
 * ONNX váha musí sedět se vstupem preprocessoru (**log-mel 80**) a výstupem **192** floatů.
 *
 * Bez souboru v assets build projde, ale [`EcapaOnnxSpeakerEmbeddingExtractor`] vrátí chybu při inference.
 */
object EcapaModelAssets {
    const val ASSET_DIR = "voiceid"
    const val ONNX_EMBED_FILE = "ecapa_embedding.onnx"

    /** Relativně k `assetDir` jako u `AssetManager.list`. */
    fun relativeOnnxPath(): String = "$ASSET_DIR/$ONNX_EMBED_FILE"

    fun bundled(context: Context): Boolean {
        val app = context.applicationContext
        val listing = runCatching { app.assets.list(ASSET_DIR) }.getOrNull() ?: return false
        return ONNX_EMBED_FILE in listing
    }
}
