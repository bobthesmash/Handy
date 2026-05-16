package cz.handy.feature.ui

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.handy.feature.voiceid.verify.VerificationThresholdStore
import cz.handy.feature.voiceid.verify.VerificationThresholds

@Composable
fun BuildIsDebuggable(): Boolean {
    val ctx = LocalContext.current
    return (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

@Composable
fun DebugVerificationScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { VerificationThresholdStore(context) }
    val persisted by store.thresholds.collectAsState()

    var tHigh by remember { mutableFloatStateOf(persisted.cosineHigh) }
    var tLow by remember { mutableFloatStateOf(persisted.cosineLow) }

    var tAntiSpoof by remember { mutableFloatStateOf(persisted.antiSpoofRejectAbove) }

    LaunchedEffect(persisted.cosineHigh, persisted.cosineLow, persisted.antiSpoofRejectAbove) {
        tHigh = persisted.cosineHigh
        tLow = persisted.cosineLow
        tAntiSpoof = persisted.antiSpoofRejectAbove
    }

    Column(modifier.fillMaxWidth()) {
        DebugVerifyTopBar(onBack = onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.debug_verify_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.debug_verify_t_high, tHigh),
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = tHigh,
                onValueChange = { v ->
                    tHigh = v.coerceIn(-1f, 1f).coerceAtLeast(tLow)
                },
                valueRange = -1f..1f,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.debug_verify_t_low, tLow),
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = tLow,
                onValueChange = { v ->
                    tLow = v.coerceIn(-1f, 1f).coerceAtMost(tHigh)
                },
                valueRange = -1f..1f,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.debug_verify_anti_spoof, tAntiSpoof),
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = tAntiSpoof,
                onValueChange = { v -> tAntiSpoof = v.coerceIn(0f, 1f) },
                valueRange = 0f..1f,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    store.update {
                        VerificationThresholds(
                            cosineHigh = tHigh,
                            cosineLow = tLow,
                            antiSpoofRejectAbove = tAntiSpoof,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.debug_verify_apply))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { store.resetToDefaults() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.debug_verify_reset_defaults))
            }
        }
    }
}

@Composable
private fun DebugVerifyTopBar(onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.debug_verify_nav_back))
            }
            Text(
                text = stringResource(R.string.debug_verify_title),
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
