package cz.handy.feature.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp

@Composable
fun AppVersionFooterText(
    modifier: Modifier = Modifier,
    spacerBeforeWhenPresent: Dp? = null,
) {
    val inspectionMode = LocalInspectionMode.current
    val appContext = LocalContext.current.applicationContext
    val line =
        remember(appContext, inspectionMode) {
            if (inspectionMode) null else installedAppFooterLine(appContext)
        }
    line?.let { footer ->
        Column(modifier = modifier) {
            spacerBeforeWhenPresent?.let { h ->
                Spacer(modifier = Modifier.height(h))
            }
            AppVersionCopyableFooterLine(appContext = appContext, footerText = footer)
        }
    }
}

@Composable
private fun AppVersionCopyableFooterLine(
    appContext: Context,
    footerText: String,
) {
    val a11yLine = stringResource(R.string.settings_version_footer_long_press_a11y, footerText)
    val copiedToast = stringResource(R.string.settings_version_copied_toast)

    fun copyVersionLine() {
        val cm = appContext.getSystemService(ClipboardManager::class.java) ?: return
        cm.setPrimaryClip(ClipData.newPlainText("Handy-verze", footerText))
        Toast.makeText(appContext, copiedToast, Toast.LENGTH_SHORT).show()
    }

    val versionTouchModifier =
        Modifier.pointerInput(footerText) {
            detectTapGestures(onLongPress = { copyVersionLine() })
        }

    Text(
        text = footerText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = versionTouchModifier.semantics { contentDescription = a11yLine },
    )
}

internal fun installedAppFooterLine(appContext: Context): String? =
    runCatching {
        val pm = appContext.packageManager
        val pkg = appContext.packageName
        val pi =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }
        val versionName = pi.versionName ?: "?"
        val versionCode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pi.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pi.versionCode.toLong()
            }
        appContext.getString(R.string.settings_build_footer, versionName, versionCode.toString())
    }.getOrNull()
