package cz.handy.feature.ui.onnx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BundledOnnxBundleHealthTest {
    @Test
    fun gaps_allPresent_empty() {
        assertEquals(
            emptySet(),
            BundledOnnxBundleHealth.gaps(
                ecapaBundled = true,
                sileroBundled = true,
                voskCsBundled = true,
                sherpaBundled = false,
            ),
        )
    }

    @Test
    fun gaps_reflectsIndependentFlags() {
        assertEquals(
            setOf(
                OnnxBundleGap.ECAPA_EMBEDDING,
                OnnxBundleGap.SILERO_VAD,
                OnnxBundleGap.VOSK_CS,
            ),
            BundledOnnxBundleHealth.gaps(false, false, false, false),
        )
        assertEquals(
            emptySet(),
            BundledOnnxBundleHealth.gaps(true, true, false, true),
        )
    }

    @Test
    fun clipboardPlainText_ordersEcapaBeforeSherpaPaths() {
        val text =
            BundledOnnxBundleHealth.clipboardPlainTextMissing(
                setOf(OnnxBundleGap.SHERPA_ZIPFORMER, OnnxBundleGap.ECAPA_EMBEDDING),
            )
        assertTrue(text.contains("ecapa_embedding.onnx"))
        assertTrue(text.contains("encoder.onnx"))
        val iEcapa = text.indexOf("ecapa_embedding.onnx")
        val iEnc = text.indexOf("encoder.onnx")
        assertTrue(iEcapa in 0 until iEnc)
    }

    @Test
    fun clipboardPlainText_empty_whenNoGaps() {
        assertEquals(
            "",
            BundledOnnxBundleHealth.clipboardPlainTextMissing(emptySet()),
        )
    }
}
