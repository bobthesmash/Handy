package cz.handy.feature.asr

import android.content.Context
import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Český Vosk small (Rhasspy) — [vosk-model-small-cs-0.4-rhasspy](https://alphacephei.com/vosk/models).
 *
 * V assets je strom Kaldi modelu (ne ONNX zipformer). Při prvním běhu se zkopíruje do `filesDir`.
 */
object VoskCzModelAssets {
    const val ASSET_PREFIX = "asr/vosk_cs_small"

    private const val FILES_SUBDIR = "vosk_cs_small"

    private val REQUIRED_TOP_LEVEL = arrayOf("am", "conf", "graph")

    fun isBundled(assetManager: AssetManager): Boolean {
        val top = assetManager.list(ASSET_PREFIX)?.toSet().orEmpty()
        if (top.isEmpty()) return false
        if (!REQUIRED_TOP_LEVEL.all(top::contains)) return false
        val am = assetManager.list("$ASSET_PREFIX/am") ?: return false
        return am.contains("final.mdl")
    }

    fun isBundled(context: Context): Boolean = isBundled(context.assets)

    private val copyLock = Any()

    /** Cesta k rozbalenému modelu na disku zařízení (kopie z assets při prvním volání). */
    @Throws(IOException::class)
    fun resolveOnDeviceModelDir(context: Context): File {
        val dest = File(context.applicationContext.filesDir, FILES_SUBDIR)
        val marker = File(dest, "am/final.mdl")
        if (marker.isFile) return dest
        synchronized(copyLock) {
            if (marker.isFile) return dest
            copyAssetTree(context.assets, ASSET_PREFIX, dest)
        }
        if (!marker.isFile) {
            throw IOException("Vosk model incomplete after copy: ${marker.absolutePath}")
        }
        return dest
    }

    private fun copyAssetTree(
        assets: AssetManager,
        assetPath: String,
        destDir: File,
    ) {
        destDir.mkdirs()
        val children = assets.list(assetPath) ?: return
        if (children.isEmpty()) {
            copyAssetFile(assets, assetPath, File(destDir, assetPath.substringAfterLast('/')))
            return
        }
        for (name in children) {
            copyAssetTree(assets, "$assetPath/$name", File(destDir, name))
        }
    }

    private fun copyAssetFile(
        assets: AssetManager,
        assetPath: String,
        dest: File,
    ) {
        dest.parentFile?.mkdirs()
        assets.open(assetPath).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }
}
