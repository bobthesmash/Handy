package cz.handy.feature.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cz.handy.feature.tts.AndroidCzechSpeechSynthesizer
import kotlinx.coroutines.delay

@Composable
fun PermissionsOnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val steps = remember { buildOnboardingSteps() }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var grantEpoch by remember { mutableIntStateOf(0) }
    var narrateEpoch by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index]

    val speech = remember { AndroidCzechSpeechSynthesizer(activity.applicationContext) }
    DisposableEffect(speech) {
        onDispose { speech.shutdown() }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            grantEpoch++
        }

    val missing =
        remember(step, grantEpoch) {
            step.permissions.filter {
                ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }

    val missingKey = remember(missing) { missing.sorted().joinToString() }
    LaunchedEffect(index, grantEpoch, missingKey, narrateEpoch) {
        speech.stop()
        delay(380)
        val narration =
            buildOnboardingNarration(
                context = context,
                stepsSize = steps.size,
                index = index,
                step = step,
                missingPermissions = missing,
            )
        speech.speak(narration) { }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.onb_progress, index + 1, steps.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (index + 1f) / steps.size },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { narrateEpoch++ },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.onb_tts_repeat))
            }
            Spacer(Modifier.height(12.dp))
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(step.title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(step.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (step) {
                    OnboardingFinish -> {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.onb_finish))
                        }
                    }

                    OnboardingNotificationListener -> {
                        Button(
                            onClick = {
                                activity.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.onb_nls_open))
                        }
                        Button(
                            onClick = { index++ },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.onb_next))
                        }
                    }

                    OnboardingIntro -> {
                        Button(
                            onClick = { index++ },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.onb_intro_cta))
                        }
                    }

                    else -> {
                        if (missing.isNotEmpty()) {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(missing.toTypedArray())
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.onb_perm_grant))
                            }
                        } else {
                            Button(
                                onClick = { index++ },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.onb_next))
                            }
                        }
                        if (step.skippable) {
                            TextButton(
                                onClick = { index++ },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.onb_skip))
                            }
                        }
                        if (step.continueWithoutPermissions && missing.isNotEmpty()) {
                            TextButton(
                                onClick = { index++ },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.onb_mic_skip))
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface OnboardingStepModel {
    @get:StringRes
    val title: Int

    @get:StringRes
    val body: Int
    val permissions: List<String> get() = emptyList()
    val skippable: Boolean get() = false
    val continueWithoutPermissions: Boolean get() = false
    val openNotificationListenerSettings: Boolean get() = false
}

private data object OnboardingIntro : OnboardingStepModel {
    override val title: Int = R.string.onb_intro_title
    override val body: Int = R.string.onb_intro_body
}

private data object OnboardingMic : OnboardingStepModel {
    override val title: Int = R.string.onb_mic_title
    override val body: Int = R.string.onb_mic_body
    override val permissions: List<String> = listOf(Manifest.permission.RECORD_AUDIO)
    override val continueWithoutPermissions: Boolean = true
}

private data object OnboardingPostNotifications : OnboardingStepModel {
    override val title: Int = R.string.onb_notif_title
    override val body: Int = R.string.onb_notif_body
    override val permissions: List<String> = listOf(Manifest.permission.POST_NOTIFICATIONS)
    override val skippable: Boolean = true
}

private data object OnboardingPhoneCluster : OnboardingStepModel {
    override val title: Int = R.string.onb_phone_title
    override val body: Int = R.string.onb_phone_body
    override val permissions: List<String> =
        listOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
        )
    override val skippable: Boolean = true
}

private data object OnboardingCamera : OnboardingStepModel {
    override val title: Int = R.string.onb_camera_title
    override val body: Int = R.string.onb_camera_body
    override val permissions: List<String> = listOf(Manifest.permission.CAMERA)
    override val skippable: Boolean = true
}

private data object OnboardingBluetooth : OnboardingStepModel {
    override val title: Int = R.string.onb_bt_title
    override val body: Int = R.string.onb_bt_body
    override val permissions: List<String> = listOf(Manifest.permission.BLUETOOTH_CONNECT)
    override val skippable: Boolean = true
}

private data object OnboardingNotificationListener : OnboardingStepModel {
    override val title: Int = R.string.onb_nls_title
    override val body: Int = R.string.onb_nls_body
    override val openNotificationListenerSettings: Boolean = true
}

private data object OnboardingFinish : OnboardingStepModel {
    override val title: Int = R.string.onb_finish_title
    override val body: Int = R.string.onb_finish_body
}

private fun buildOnboardingNarration(
    context: Context,
    stepsSize: Int,
    index: Int,
    step: OnboardingStepModel,
    missingPermissions: List<String>,
): String {
    val prefix = context.getString(R.string.onb_tts_step_prefix, index + 1, stepsSize)
    val headline = context.getString(step.title)
    val detail = context.getString(step.body)
    val tail =
        when (step) {
            OnboardingIntro -> context.getString(R.string.onb_tts_tail_intro)
            OnboardingFinish -> context.getString(R.string.onb_tts_tail_finish)
            OnboardingNotificationListener -> context.getString(R.string.onb_tts_tail_nls)
            OnboardingMic ->
                if (missingPermissions.isNotEmpty()) {
                    context.getString(R.string.onb_tts_tail_perm_mic)
                } else {
                    context.getString(R.string.onb_tts_tail_next)
                }
            else ->
                when {
                    step.permissions.isEmpty() -> context.getString(R.string.onb_tts_tail_next)
                    missingPermissions.isNotEmpty() ->
                        context.getString(R.string.onb_tts_tail_perm_optional)
                    else -> context.getString(R.string.onb_tts_tail_next)
                }
        }
    return "$prefix $headline. $detail $tail"
}

private fun buildOnboardingSteps(): List<OnboardingStepModel> =
    buildList {
        add(OnboardingIntro)
        add(OnboardingMic)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(OnboardingPostNotifications)
        }
        add(OnboardingPhoneCluster)
        add(OnboardingCamera)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(OnboardingBluetooth)
        }
        add(OnboardingNotificationListener)
        add(OnboardingFinish)
    }
