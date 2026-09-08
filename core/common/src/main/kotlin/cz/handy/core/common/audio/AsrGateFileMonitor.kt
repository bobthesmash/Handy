package cz.handy.core.common.audio

import java.io.File

/**
 * Reloads [AsrGatePolicy] from an on-device JSON file when the path or mtime changes.
 * Missing or unreadable files yield [AsrGatePolicy.DEFAULT] without throwing.
 */
class AsrGateFileMonitor(
    private val primaryFile: File?,
    private val fallbackFile: File?,
) {
    private var cacheKey: String = UNSET_CACHE_KEY
    private var cacheMtime: Long = Long.MIN_VALUE
    private var cachePolicy: AsrGatePolicy = AsrGatePolicy.DEFAULT

    fun snapshot(): AsrGateSnapshot {
        val file = pickFile()
        val key = file?.absolutePath.orEmpty()
        val mtime = file?.lastModified() ?: 0L
        if (key == cacheKey && mtime == cacheMtime) {
            return AsrGateSnapshot(
                policy = cachePolicy,
                sourcePath = file?.absolutePath,
                fromCache = true,
            )
        }
        val policy =
            if (file == null) {
                AsrGatePolicy.DEFAULT
            } else {
                runCatching { AsrGatePolicy.parseJson(file.readText()) }.getOrDefault(AsrGatePolicy.DEFAULT)
            }
        cacheKey = key
        cacheMtime = mtime
        cachePolicy = policy
        return AsrGateSnapshot(
            policy = policy,
            sourcePath = file?.absolutePath,
            fromCache = false,
        )
    }

    private fun pickFile(): File? {
        if (primaryFile != null && primaryFile.isFile) return primaryFile
        if (fallbackFile != null && fallbackFile.isFile) return fallbackFile
        return null
    }

    companion object {
        const val FILE_NAME: String = "asr_gate.json"
        private const val UNSET_CACHE_KEY: String = "<unset>"
    }
}

data class AsrGateSnapshot(
    val policy: AsrGatePolicy,
    val sourcePath: String?,
    val fromCache: Boolean,
)
