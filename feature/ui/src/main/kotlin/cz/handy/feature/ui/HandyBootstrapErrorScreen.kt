package cz.handy.feature.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun HandyBootstrapErrorScreen(
    error: Throwable?,
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val detail =
        error?.let { "${it.javaClass.simpleName}: ${it.message}\n\n${it.stackTraceToString().take(4000)}" }
            ?: stringResource(R.string.bootstrap_error_unknown)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.bootstrap_error_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.bootstrap_error_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    ContextCompat
                        .getSystemService(context, ClipboardManager::class.java)
                        ?.setPrimaryClip(ClipData.newPlainText("Handy bootstrap", detail))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.bootstrap_error_copy))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onResetOnboarding,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.bootstrap_error_reset_onboarding))
            }
        }
    }
}
