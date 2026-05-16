package cz.handy.feature.actions.torch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

/** Svítilna přes [`CameraManager.setTorchMode`] ([F1-T14]). */
class TorchModeSwitcher(
    context: Context,
) {
    private val app = context.applicationContext
    private val cam = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    @RequiresPermission(Manifest.permission.CAMERA)
    fun setTorch(enabled: Boolean): Result<String> {
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure(
                SecurityException("${Manifest.permission.CAMERA}: permission not granted."),
            )
        }
        val id =
            preferredBackCameraWithFlashId()
                ?: return Result.failure(
                    IllegalStateException("Zařízení nemá použitelnou zadní lampu."),
                )

        return runCatching {
            cam.setTorchMode(id, enabled)
            if (enabled) "Svítilna zapnutá." else "Svítilna vypnutá."
        }
    }

    private fun preferredBackCameraWithFlashId(): String? =
        cam.cameraIdList.firstOrNull { cid ->
            val facing =
                cam.getCameraCharacteristics(cid).get(CameraCharacteristics.LENS_FACING)
            facing == CameraCharacteristics.LENS_FACING_BACK &&
                cam.getCameraCharacteristics(cid).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
}
