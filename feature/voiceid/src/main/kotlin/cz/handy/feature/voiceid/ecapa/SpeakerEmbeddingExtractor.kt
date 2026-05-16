package cz.handy.feature.voiceid.ecapa

/**
 * Offline extraktor **192‑D** vektorů pro ověření mluvčího ([F1‑T02+]).
 */
fun interface SpeakerEmbeddingExtractor {
    /** Mono PCM16 vzorkované typicky @16 kHz; viz [SpeechbrainEcapaPreprocessor] (F1-T02). */
    fun embedPcm16(pcmMono16Le: ShortArray): Result<FloatArray>
}
