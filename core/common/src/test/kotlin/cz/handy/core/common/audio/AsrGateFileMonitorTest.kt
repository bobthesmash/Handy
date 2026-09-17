package cz.handy.core.common.audio

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AsrGateFileMonitorTest {
    @Test
    fun missingFiles_returnDefaultsAndCache() {
        val dir = Files.createTempDirectory("asr-gate-missing-").toFile()
        try {
            val monitor =
                AsrGateFileMonitor(
                    primaryFile = File(dir, "primary.json"),
                    fallbackFile = File(dir, "fallback.json"),
                )
            val first = monitor.snapshot()
            assertEquals(AsrGatePolicy.DEFAULT, first.policy)
            assertNull(first.sourcePath)
            assertFalse(first.fromCache)
            val second = monitor.snapshot()
            assertTrue(second.fromCache)
            assertEquals(AsrGatePolicy.DEFAULT, second.policy)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun prefersPrimaryOverFallback() {
        val dir = Files.createTempDirectory("asr-gate-pref-").toFile()
        try {
            val primary = File(dir, AsrGateFileMonitor.FILE_NAME)
            val fallback = File(dir, "fallback.json")
            primary.writeText("""{"mode":"prob","minProb":0.9}""")
            fallback.writeText("""{"mode":"off"}""")
            val monitor = AsrGateFileMonitor(primary, fallback)
            val snap = monitor.snapshot()
            assertEquals(AsrGateMode.PROB, snap.policy.mode)
            assertEquals(0.9f, snap.policy.minProb, ABS_TOLERANCE)
            assertEquals(primary.absolutePath, snap.sourcePath)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun usesFallbackWhenPrimaryMissing() {
        val dir = Files.createTempDirectory("asr-gate-fb-").toFile()
        try {
            val fallback = File(dir, AsrGateFileMonitor.FILE_NAME)
            fallback.writeText("""{"enabled":false}""")
            val monitor = AsrGateFileMonitor(File(dir, "missing.json"), fallback)
            val snap = monitor.snapshot()
            assertEquals(false, snap.policy.enabled)
            assertEquals(fallback.absolutePath, snap.sourcePath)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun reloadsWhenMtimeChanges() {
        val dir = Files.createTempDirectory("asr-gate-mtime-").toFile()
        try {
            val primary = File(dir, AsrGateFileMonitor.FILE_NAME)
            primary.writeText("""{"mode":"prob","minProb":0.9}""")
            val monitor = AsrGateFileMonitor(primary, null)
            val first = monitor.snapshot()
            assertEquals(0.9f, first.policy.minProb, ABS_TOLERANCE)
            assertFalse(first.fromCache)
            assertTrue(monitor.snapshot().fromCache)

            primary.writeText("""{"mode":"logprob","minProb":0.11}""")
            assertTrue(primary.setLastModified(primary.lastModified() + MTIME_BUMP_MS))
            val reloaded = monitor.snapshot()
            assertFalse(reloaded.fromCache)
            assertEquals(AsrGateMode.LOGPROB, reloaded.policy.mode)
            assertEquals(0.11f, reloaded.policy.minProb, ABS_TOLERANCE)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun unreadableOrInvalidFile_fallsBackToDefaults() {
        val dir = Files.createTempDirectory("asr-gate-bad-").toFile()
        try {
            val primary = File(dir, AsrGateFileMonitor.FILE_NAME)
            primary.writeText("{{{{")
            val monitor = AsrGateFileMonitor(primary, null)
            val snap = monitor.snapshot()
            assertEquals(AsrGatePolicy.DEFAULT.enabled, snap.policy.enabled)
            assertEquals(AsrGatePolicy.DEFAULT.mode, snap.policy.mode)
            assertEquals(primary.absolutePath, snap.sourcePath)
        } finally {
            dir.deleteRecursively()
        }
    }

    companion object {
        private const val ABS_TOLERANCE = 1e-5f
        private const val MTIME_BUMP_MS = 2_000L
    }
}
