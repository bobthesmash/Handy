package cz.handy.feature.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Locale

private enum class PrivacyPolicyLocaleTag {
    CS,
    EN,
}

@Composable
fun PrivacyPolicyScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var localeTag by rememberSaveable {
        mutableStateOf(
            if (Locale.getDefault().language.lowercase(Locale.ROOT) == "cs") {
                PrivacyPolicyLocaleTag.CS
            } else {
                PrivacyPolicyLocaleTag.EN
            },
        )
    }
    val policyText =
        remember(localeTag) {
            val resId =
                when (localeTag) {
                    PrivacyPolicyLocaleTag.CS -> R.raw.privacy_policy_cs
                    PrivacyPolicyLocaleTag.EN -> R.raw.privacy_policy_en
                }
            context.resources
                .openRawResource(resId)
                .bufferedReader()
                .use { it.readText() }
        }
    val scroll = rememberScrollState()

    Column(modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 10.dp),
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.privacy_policy_nav_back))
                }
                Text(
                    text = stringResource(R.string.privacy_policy_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            TextButton(
                onClick = { localeTag = PrivacyPolicyLocaleTag.CS },
                enabled = localeTag != PrivacyPolicyLocaleTag.CS,
            ) {
                Text(stringResource(R.string.privacy_policy_lang_cs))
            }
            TextButton(
                onClick = { localeTag = PrivacyPolicyLocaleTag.EN },
                enabled = localeTag != PrivacyPolicyLocaleTag.EN,
            ) {
                Text(stringResource(R.string.privacy_policy_lang_en))
            }
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll),
        ) {
            Text(
                text = policyText,
                style = MaterialTheme.typography.bodyMedium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
