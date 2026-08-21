package cz.handy.feature.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.handy.feature.ui.onnx.BundledOnnxBundleHealth
import cz.handy.feature.voiceid.storage.SpeakerEmbeddingEncryptedStore
import cz.handy.feature.wakeword.WakeWordAvailability

@Composable
fun HomePipelineHintsBanner(
    showCommandPipelineUi: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext
    val wakeConfigured = remember { WakeWordAvailability.isPicovoiceKeyConfigured() }
    val hasProfile =
        remember {
            SpeakerEmbeddingEncryptedStore(app).hasSpeakerProfile()
        }
    val asrBundled = remember(app) { BundledOnnxBundleHealth.isSherpaListeningPossible(app) }

    val lines = buildList {
        if (!wakeConfigured) {
            add(stringResource(R.string.home_hint_wake_disabled))
        } else {
            add(
                stringResource(
                    R.string.home_hint_wake_enabled,
                    WakeWordAvailability.BUILTIN_KEYWORD_LABEL,
                ),
            )
        }
        if (!hasProfile) {
            add(stringResource(R.string.home_hint_no_voice_profile))
        }
        if (!asrBundled) {
            add(stringResource(R.string.home_hint_no_asr_models))
        }
        if (showCommandPipelineUi) {
            add(stringResource(R.string.home_hint_debug_commands))
        } else if (hasProfile && asrBundled) {
            add(stringResource(R.string.home_hint_release_voice))
        }
    }
    if (lines.isEmpty()) return

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        lines.forEachIndexed { index, line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = if (index == 0) 10.dp else 4.dp,
                        )
                        .then(
                            if (index == lines.lastIndex) {
                                Modifier.padding(bottom = 10.dp)
                            } else {
                                Modifier
                            },
                        ),
            )
        }
    }
}
