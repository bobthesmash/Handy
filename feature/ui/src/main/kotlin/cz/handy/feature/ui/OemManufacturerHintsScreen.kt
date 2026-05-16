package cz.handy.feature.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun OemManufacturerHintsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Column(modifier.fillMaxWidth()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 10.dp),
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.oem_hints_nav_back))
                }
                Text(
                    text = stringResource(R.string.oem_hints_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.oem_hints_intro),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.oem_hints_device, Build.MANUFACTURER),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OemHintLink(
                title = stringResource(R.string.oem_vendor_generic),
                url = "https://dontkillmyapp.com/",
                context = context,
            )
            OemHintLink(
                title = stringResource(R.string.oem_vendor_samsung),
                url = "https://dontkillmyapp.com/samsung",
                context = context,
            )
            OemHintLink(
                title = stringResource(R.string.oem_vendor_xiaomi),
                url = "https://dontkillmyapp.com/xiaomi",
                context = context,
            )
            OemHintLink(
                title = stringResource(R.string.oem_vendor_huawei),
                url = "https://dontkillmyapp.com/huawei",
                context = context,
            )
            OemHintLink(
                title = stringResource(R.string.oem_vendor_oneplus),
                url = "https://dontkillmyapp.com/oneplus",
                context = context,
            )
            OemHintLink(
                title = stringResource(R.string.oem_vendor_oppo),
                url = "https://dontkillmyapp.com/oppo",
                context = context,
            )
            OemHintLink(
                title = stringResource(R.string.oem_vendor_vivo),
                url = "https://dontkillmyapp.com/vivo",
                context = context,
            )
        }
    }
}

@Composable
private fun OemHintLink(
    title: String,
    url: String,
    context: Context,
) {
    OutlinedCard(
        onClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
