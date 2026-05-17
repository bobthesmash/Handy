package cz.handy.feature.ui.onnx

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cz.handy.feature.ui.R

/**
 * Shrnutí chybějících ONNX v APK pro vývojáře (popis kde soubory v repu doplnit, bez jejich stahování z aplikace).
 */
@Composable
internal fun OnnxMissingModelsBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext
    val gaps =
        remember(app) {
            BundledOnnxBundleHealth.gaps(app)
        }

    if (gaps.isEmpty()) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.onnx_models_missing_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onnx_models_missing_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.92f),
            )

            Spacer(Modifier.height(10.dp))

            gaps
                .toList()
                .sortedBy { it.ordinal }
                .forEach { gap ->
                    Text(
                        text = "• ${onnxGapShortLabel(gap)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )

                    gap.relativeDeveloperPathsUnix().forEach { path ->
                        Text(
                            text = path,
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp),
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                }

            OutlinedButton(
                onClick = {
                    val text =
                        BundledOnnxBundleHealth.clipboardPlainTextMissing(gaps)

                    ContextCompat
                        .getSystemService(context, ClipboardManager::class.java)
                        ?.setPrimaryClip(
                            ClipData.newPlainText("Handy ONNX paths", text),
                        )

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        Toast
                            .makeText(
                                context,
                                context.getText(R.string.onnx_models_paths_copied_toast),
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.onnx_models_copy_paths))
            }

            Text(
                text =
                    stringResource(
                        R.string.onnx_models_readme_hint,
                    ),
                style = MaterialTheme.typography.labelSmall,
                color =
                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun onnxGapShortLabel(gap: OnnxBundleGap): String =
    when (gap) {
        OnnxBundleGap.ECAPA_EMBEDDING ->
            stringResource(R.string.onnx_models_gap_ecapa)
        OnnxBundleGap.SILERO_VAD ->
            stringResource(R.string.onnx_models_gap_silero_vad)
        OnnxBundleGap.VOSK_CS ->
            stringResource(R.string.onnx_models_gap_vosk_cs)
        OnnxBundleGap.SHERPA_ZIPFORMER ->
            stringResource(R.string.onnx_models_gap_sherpa_zipformer)
    }
