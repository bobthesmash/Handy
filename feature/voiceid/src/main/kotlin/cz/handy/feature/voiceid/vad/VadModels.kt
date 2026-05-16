package cz.handy.feature.voiceid.vad

/** Jeden ONNX krok Silero VAD (typicky 512 vzorků @ 16 kHz). */
data class VadFrame(
    val startInclusive: Int,
    val endExclusive: Int,
    val speechProb: Float,
) {
    init {
        require(endExclusive > startInclusive) { "Frame must cover a positive sample span." }
    }
}

/** Spojitý úsek řeči ve vzorcích mono PCM. */
data class VadSegment(
    val startInclusive: Int,
    val endExclusive: Int,
) {
    init {
        require(endExclusive > startInclusive) { "Segment must cover a positive sample span." }
    }

    val lengthSamples: Int
        get() = endExclusive - startInclusive
}
