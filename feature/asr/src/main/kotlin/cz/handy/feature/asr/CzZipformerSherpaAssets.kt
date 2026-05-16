package cz.handy.feature.asr

import android.content.Context
import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig

/**
 * Očekávané ONNX soubory pod `assets/cs_zipformer_small/`, struktura jako sherpa vosk-derived **zipformer2**
 * (viz např. `sherpa-onnx-streaming-zipformer-small-ru-vosk-*` na Hugging Face / ADR‑0004).
 *
 * Produkt cílí češtinu; před dostupností českého exportu lze adresář dočasně naplnit jiným **zipformer2**
 * stream transducer balíčkem → stejné soubory a názvy.
 */
object CzZipformerSherpaAssets {
    const val PREFIX = "asr/cs_zipformer_small"

    private val REQUIRED_FILENAMES =
        arrayOf(
            "tokens.txt",
            "encoder.onnx",
            "decoder.onnx",
            "joiner.onnx",
        )

    /** Zda jsou vyplněná minima nutná pro inicializaci [OnlineRecognizer] z Assetů. */
    fun isBundled(assetManager: AssetManager): Boolean {
        val raw = assetManager.list(PREFIX)
        val listing = raw?.toHashSet().orEmpty()
        return raw != null && listing.isNotEmpty() && REQUIRED_FILENAMES.all(listing::contains)
    }

    fun isBundled(context: Context): Boolean = isBundled(context.assets)

    internal fun onlineModelConfig(threads: Int = 2): OnlineModelConfig =
        OnlineModelConfig(
            transducer =
                OnlineTransducerModelConfig(
                    encoder = "$PREFIX/encoder.onnx",
                    decoder = "$PREFIX/decoder.onnx",
                    joiner = "$PREFIX/joiner.onnx",
                ),
            tokens = "$PREFIX/tokens.txt",
            modelType = ZIPFORMER2,
            numThreads = threads,
            provider = "cpu",
        )

    private const val ZIPFORMER2 = "zipformer2"
}
