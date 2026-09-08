package cz.handy.core.common.audio

import java.io.File

/**
 * Reloads [AsrGateJson] when the on-device override file's mtime changes.
 * [preferred] is typically `getExternalFilesDir(null)/asr_gate.json` (adb-pushable);
 * [fallback] is `filesDir/asr_gate.json`. Missing/unreadable files → [AsrGatePolicy.DEFAULT].
 */
class AsrGateHotFile(
    private val preferred: File?,
    private val fallback: File,
) {
    private data class CacheKey(
        val path: String,
        val mtime: Long,
    )

    private var cacheKey: CacheKey? = null
    private var cached: AsrGatePolicy = AsrGatePolicy.DEFAULT

    fun current(): AsrGatePolicy {
        val file = resolveReadableFile()
        if (file == null) {
            cacheKey = null
            cached = AsrGatePolicy.DEFAULT
            return cached
        }
        val key = CacheKey(file.absolutePath, file.lastModified())
        if (key == cacheKey) return cached
        cached =
            runCatching { AsrGateJson.parse(file.readText()) }
                .getOrElse { AsrGatePolicy.DEFAULT }
        cacheKey = key
        return cached
    }

    private fun resolveReadableFile(): File? {
        if (preferred != null && preferred.isFile) return preferred
        if (fallback.isFile) return fallback
        return null
    }

    companion object {
        const val FILE_NAME = "asr_gate.json"
    }
}
