package cz.handy.feature.wakeword

import kotlin.test.Test
import kotlin.test.assertEquals

class WakeWordInferenceBudgetTest {
    @Test
    fun samplesPerTick_is480_at16k30ms() {
        assertEquals(480, WakeWordInferenceBudget.samplesPerTickAt16k)
        assertEquals(30, WakeWordInferenceBudget.INFERENCE_INTERVAL_MS)
    }
}
