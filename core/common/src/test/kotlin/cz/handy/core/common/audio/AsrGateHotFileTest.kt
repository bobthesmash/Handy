package cz.handy.core.common.audio

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class AsrGateHotFileTest {
    @Test
    fun missingFiles_returnDefaults() {
        withTempDir { dir ->
            val hot =
                AsrGateHotFile(
                    preferred = File(dir, AsrGateHotFile.FILE_NAME),
                    fallback = File(dir, "internal-${AsrGateHotFile.FILE_NAME}"),
                )
            assertEquals(AsrGatePolicy.DEFAULT, hot.current())
        }
    }

    @Test
    fun prefersExternalFileOverInternalFallback() {
        withTempDir { dir ->
            val preferred = File(dir, AsrGateHotFile.FILE_NAME)
            val fallback = File(dir, "internal-${AsrGateHotFile.FILE_NAME}")
            preferred.writeText("""{"mode":"off"}""")
            fallback.writeText("""{"mode":"prob","minProb":0.9}""")
            val hot = AsrGateHotFile(preferred, fallback)
            assertEquals(AsrGateMode.OFF, hot.current().mode)
        }
    }

    @Test
    fun usesFallbackWhenPreferredMissing() {
        withTempDir { dir ->
            val preferred = File(dir, AsrGateHotFile.FILE_NAME)
            val fallback = File(dir, "internal-${AsrGateHotFile.FILE_NAME}")
            fallback.writeText("""{"mode":"prob","minProb":0.5}""")
            val hot = AsrGateHotFile(preferred, fallback)
            val policy = hot.current()
            assertEquals(AsrGateMode.PROB, policy.mode)
            assertEquals(0.5f, policy.minProb)
        }
    }

    @Test
    fun invalidFile_returnsDefaultsWithoutThrowing() {
        withTempDir { dir ->
            val preferred = File(dir, AsrGateHotFile.FILE_NAME)
            preferred.writeText("{{{{{")
            val hot = AsrGateHotFile(preferred, File(dir, "unused.json"))
            assertEquals(AsrGatePolicy.DEFAULT, hot.current())
        }
    }

    @Test
    fun reloadsWhenMtimeChanges() {
        withTempDir { dir ->
            val preferred = File(dir, AsrGateHotFile.FILE_NAME)
            val hot = AsrGateHotFile(preferred, File(dir, "unused.json"))

            preferred.writeText("""{"mode":"off"}""")
            preferred.setLastModified(1_000L)
            assertEquals(AsrGateMode.OFF, hot.current().mode)

            preferred.writeText("""{"mode":"prob","minProb":0.55}""")
            preferred.setLastModified(2_000L)
            val reloaded = hot.current()
            assertEquals(AsrGateMode.PROB, reloaded.mode)
            assertEquals(0.55f, reloaded.minProb)
        }
    }

    @Test
    fun sameMtimeKeepsCachedPolicy() {
        withTempDir { dir ->
            val preferred = File(dir, AsrGateHotFile.FILE_NAME)
            val hot = AsrGateHotFile(preferred, File(dir, "unused.json"))

            preferred.writeText("""{"mode":"off"}""")
            preferred.setLastModified(5_000L)
            assertEquals(AsrGateMode.OFF, hot.current().mode)

            preferred.writeText("""{"mode":"prob"}""")
            preferred.setLastModified(5_000L)
            assertEquals(AsrGateMode.OFF, hot.current().mode)
        }
    }

    @Test
    fun deletedFileAfterCache_returnsDefaults() {
        withTempDir { dir ->
            val preferred = File(dir, AsrGateHotFile.FILE_NAME)
            val hot = AsrGateHotFile(preferred, File(dir, "unused.json"))
            preferred.writeText("""{"mode":"off"}""")
            preferred.setLastModified(3_000L)
            assertEquals(AsrGateMode.OFF, hot.current().mode)

            preferred.delete()
            assertEquals(AsrGatePolicy.DEFAULT, hot.current())
        }
    }

    private fun withTempDir(block: (File) -> Unit) {
        val dir = createTempDirectory("asr-gate-hot").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }
}
