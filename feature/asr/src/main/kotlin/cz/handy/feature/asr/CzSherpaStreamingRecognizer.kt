package cz.handy.feature.asr

import android.content.Context
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.getEndpointConfig
import com.k2fsa.sherpa.onnx.getFeatureConfig
import cz.handy.core.audio.MicCaptureConfig

/**
 * Továrna na streaming **zipformer2 transducer** (16 kHz, 80‑dim vlastnosti podle sherpa-onnx).
 *
 * @return `null`, pokud v assets ještě není český (nebo vývojářsky náhradní) ONNX balíček.
 */
fun createCzSherpaStreamingRecognizer(
    context: Context,
    endpointConfig: EndpointConfig = getEndpointConfig(),
    decodingMethod: String = "greedy_search",
): OnlineRecognizer? {
    val assets = context.applicationContext.assets
    if (!CzZipformerSherpaAssets.isBundled(assets)) {
        return null
    }

    val modelConfig = CzZipformerSherpaAssets.onlineModelConfig()
    val config =
        OnlineRecognizerConfig(
            featConfig =
                getFeatureConfig(
                    sampleRate = MicCaptureConfig.SAMPLE_RATE_HZ,
                    featureDim = 80,
                ),
            modelConfig = modelConfig,
            endpointConfig = endpointConfig,
            enableEndpoint = true,
            decodingMethod = decodingMethod,
        )
    return OnlineRecognizer(assetManager = assets, config = config)
}
