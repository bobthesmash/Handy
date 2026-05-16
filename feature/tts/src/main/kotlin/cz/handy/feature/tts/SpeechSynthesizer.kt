package cz.handy.feature.tts

/**
 * Jednosměrný výstup asistenta přes systémový TTS ([F1-T15]).
 *
 * Veškerá volání musí být z hlavního vlákna (Android [android.speech.tts.TextToSpeech]).
 */
interface SpeechSynthesizer {
    /** Zastaví probíhající čtení; další [speak] může začít okamžitě. */
    fun stop()

    /**
     * Přečte [text] (QUEUE_FLUSH — přeruší předchozí hlášku).
     * [onComplete] se zavolá na hlavním vlákně po dočtení, po chybě engine nebo když bylo čtení přerušeno
     * jiným [speak]/[stop] (v tom případě se nevolá pro starší utterance).
     */
    fun speak(
        text: String,
        onComplete: () -> Unit,
    )

    fun shutdown()
}
