package cz.handy.feature.ui.onnx

import android.content.Context
import cz.handy.feature.asr.CzZipformerSherpaAssets
import cz.handy.feature.voiceid.ecapa.EcapaModelAssets
import cz.handy.feature.voiceid.vad.SileroVadModelAssets

/**
 * Přítomnost binárních ONNX v aplikačních assets (stejné cesty jako v README u zdrojových assetů dev).
 *
 * Použití jen pro orientaci uživatele / vývojáře na zařízení — soubory se do APK dostanou při lokálním buildu.
 */
enum class OnnxBundleGap {
    /** [EcapaModelAssets.ONNX_EMBED_FILE] */
    ECAPA_EMBEDDING,

    /** [SileroVadModelAssets] — ONNX v `voiceid/`. */
    SILERO_VAD,

    /** [CzZipformerSherpaAssets] — adresář `asr/cs_zipformer_small/` se čtyřmi soubory. */
    SHERPA_ZIPFORMER,
    ;

    fun relativeDeveloperPathsUnix(): List<String> =
        when (this) {
            ECAPA_EMBEDDING ->
                listOf(
                    "feature/voiceid/src/main/assets/${EcapaModelAssets.relativeOnnxPath()}",
                )
            SILERO_VAD ->
                listOf(
                    "feature/voiceid/src/main/assets/voiceid/silero_vad.onnx",
                )
            SHERPA_ZIPFORMER ->
                listOf(
                    "feature/asr/src/main/assets/${CzZipformerSherpaAssets.PREFIX}/tokens.txt",
                    "feature/asr/src/main/assets/${CzZipformerSherpaAssets.PREFIX}/encoder.onnx",
                    "feature/asr/src/main/assets/${CzZipformerSherpaAssets.PREFIX}/decoder.onnx",
                    "feature/asr/src/main/assets/${CzZipformerSherpaAssets.PREFIX}/joiner.onnx",
                )
        }
}

object BundledOnnxBundleHealth {
    fun gaps(
        ecapaBundled: Boolean,
        sileroBundled: Boolean,
        sherpaBundled: Boolean,
    ): Set<OnnxBundleGap> =
        buildSet {
            if (!ecapaBundled) add(OnnxBundleGap.ECAPA_EMBEDDING)
            if (!sileroBundled) add(OnnxBundleGap.SILERO_VAD)
            if (!sherpaBundled) add(OnnxBundleGap.SHERPA_ZIPFORMER)
        }

    fun gaps(context: Context): Set<OnnxBundleGap> {
        val app = context.applicationContext
        return gaps(
            ecapaBundled = EcapaModelAssets.bundled(app),
            sileroBundled = SileroVadModelAssets.bundled(app),
            sherpaBundled = CzZipformerSherpaAssets.isBundled(app),
        )
    }

    /** Text pro systémovou schránku (bez sítě, jen cesty jako v repu). */
    fun clipboardPlainTextMissing(gaps: Set<OnnxBundleGap>): String {
        if (gaps.isEmpty()) {
            return ""
        }
        val paths =
            gaps
                .sortedBy { it.ordinal }
                .flatMap { it.relativeDeveloperPathsUnix() }
                .distinct()
                .joinToString(separator = "\n")
        return "Handy — chybějící ONNX (cesty podle struktury projektu):\n$paths"
    }

    /** Zda má smysl spouštět Sherpa příjem řeči. */
    fun isSherpaListeningPossible(context: Context): Boolean = CzZipformerSherpaAssets.isBundled(context.applicationContext)
}
