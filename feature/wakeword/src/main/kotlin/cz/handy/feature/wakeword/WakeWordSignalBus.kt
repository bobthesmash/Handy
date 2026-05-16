package cz.handy.feature.wakeword

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Jeden vstupní kanál ze smyčky wake-wordu ([PorcupineEarWakePump]) k obsluze ve [cz.handy.feature.ui.pipeline.HandyAssistantViewModel].
 */
object WakeWordSignalBus {
    private val wake =
        MutableSharedFlow<Unit>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    val wakes: SharedFlow<Unit> = wake.asSharedFlow()

    internal fun emitWakeKick() {
        wake.tryEmit(Unit)
    }
}
