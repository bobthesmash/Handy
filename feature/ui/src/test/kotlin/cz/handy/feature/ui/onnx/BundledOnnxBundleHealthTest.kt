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
                sherpaBundled = true,
            ),
        )
    }

    @Test
    fun gaps_reflectsIndependentFlags() {
        assertEquals(
            setOf(
                OnnxBundleGap.ECAPA_EMBEDDING,
                OnnxBundleGap.SILERO_VAD,
                OnnxBundleGap.SHERPA_ZIPFORMER,
            ),
            BundledOnnxBundleHealth.gaps(false, false, false),
        )
        assertEquals(
            setOf(OnnxBundleGap.SHERPA_ZIPFORMER),
            BundledOnnxBundleHealth.gaps(true, true, false),
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
