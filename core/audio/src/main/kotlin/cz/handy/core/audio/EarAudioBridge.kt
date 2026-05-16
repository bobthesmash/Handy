package cz.handy.core.audio

/**
 * Jediný writer je [EarService]; čtení pro ASR pipeline v UI vrstvě ([HandyAssistantViewModel]).
 */
object EarAudioBridge {
    @Volatile
    private var attached: MonoPcmRingBuffer? = null

    fun ringBufferOrNull(): MonoPcmRingBuffer? = attached

    internal fun attach(buffer: MonoPcmRingBuffer) {
        attached = buffer
    }

    internal fun detach(buffer: MonoPcmRingBuffer) {
        if (attached === buffer) {
            attached = null
        }
    }
}
