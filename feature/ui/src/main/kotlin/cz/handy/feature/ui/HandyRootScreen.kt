package cz.handy.feature.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.handy.core.audio.EarService
import cz.handy.core.common.dialog.DialogPhase
import cz.handy.feature.ui.onnx.BundledOnnxBundleHealth
import cz.handy.feature.ui.onnx.OnnxMissingModelsBanner
import cz.handy.feature.ui.pipeline.HandyAssistantViewModel
import cz.handy.feature.ui.theme.HandyTheme
import cz.handy.feature.voiceid.confirm.DestructiveConfirmVoiceVerifier
import cz.handy.feature.voiceid.enrollment.EnrollmentClipRecorder
import cz.handy.feature.voiceid.io.Pcm16LittleEndianIo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import java.io.File

private enum class RootRoute {
    Home,
    Enrollment,
    Settings,
    OemHints,
    Privacy,
    BetaFeedback,
    DebugVerify,
}

@Composable
fun HandyRootScreen(
    modifier: Modifier = Modifier,
    assistant: HandyAssistantViewModel? = null,
    showCommandPipelineUi: Boolean = false,
) {
    var route by rememberSaveable { mutableStateOf(RootRoute.Home.name) }

    var returnToSettingsAfterEnrollment by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    val inspectionMode = LocalInspectionMode.current

    val debugBuild = BuildIsDebuggable()

    val dialogPhaseFlow =
        remember(assistant) {
            assistant?.dialogPhase ?: flowOf(DialogPhase.Idle)
        }

    val dialogPhase by dialogPhaseFlow.collectAsStateWithLifecycle(DialogPhase.Idle)

    LaunchedEffect(debugBuild, route) {
        if (!debugBuild && route == RootRoute.DebugVerify.name) {
            route = RootRoute.Home.name
        }
    }

    LaunchedEffect(route, inspectionMode, debugBuild) {
        if (inspectionMode) return@LaunchedEffect

        val app = context.applicationContext

        val onListenSurface =
            route == RootRoute.Home.name ||
                route == RootRoute.Settings.name ||
                route == RootRoute.OemHints.name ||
                route == RootRoute.Privacy.name ||
                route == RootRoute.BetaFeedback.name ||
                (route == RootRoute.DebugVerify.name && debugBuild)

        if (route == RootRoute.Enrollment.name) {
            EarService.stop(app)
        } else if (onListenSurface &&

            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED

        ) {
            EarService.start(app)
        }
    }

    LaunchedEffect(dialogPhase, route, inspectionMode, debugBuild, assistant) {
        if (inspectionMode) return@LaunchedEffect

        if (assistant == null) return@LaunchedEffect

        val onListenSurface =
            route == RootRoute.Home.name ||
                route == RootRoute.Settings.name ||
                route == RootRoute.OemHints.name ||
                route == RootRoute.Privacy.name ||
                route == RootRoute.BetaFeedback.name ||
                (route == RootRoute.DebugVerify.name && debugBuild)

        if (!onListenSurface) return@LaunchedEffect

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) != PackageManager.PERMISSION_GRANTED

        ) {
            return@LaunchedEffect
        }

        EarService.notifyForegroundUiState(
            context,
            dialogPhase.toEarForegroundUiState(),
        )
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->

        when (route) {
            RootRoute.Enrollment.name ->

                EnrollmentScreen(
                    modifier = Modifier.padding(innerPadding),
                    onBack = {
                        if (returnToSettingsAfterEnrollment) {
                            returnToSettingsAfterEnrollment = false

                            route = RootRoute.Settings.name
                        } else {
                            route = RootRoute.Home.name
                        }
                    },
                )

            RootRoute.Settings.name ->

                SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    onBack = { route = RootRoute.Home.name },
                    onOpenEnrollment = {
                        returnToSettingsAfterEnrollment = true

                        route = RootRoute.Enrollment.name
                    },
                    onOpenOemHints = { route = RootRoute.OemHints.name },
                    onOpenPrivacyPolicy = { route = RootRoute.Privacy.name },
                    onOpenBetaFeedback = { route = RootRoute.BetaFeedback.name },
                )

            RootRoute.OemHints.name ->

                OemManufacturerHintsScreen(
                    modifier = Modifier.padding(innerPadding),
                    onBack = { route = RootRoute.Settings.name },
                )

            RootRoute.Privacy.name ->

                PrivacyPolicyScreen(
                    modifier = Modifier.padding(innerPadding),
                    onBack = { route = RootRoute.Settings.name },
                )

            RootRoute.BetaFeedback.name ->

                BetaFeedbackScreen(
                    modifier = Modifier.padding(innerPadding),
                    onBack = { route = RootRoute.Settings.name },
                )

            RootRoute.DebugVerify.name ->

                if (debugBuild) {
                    DebugVerificationScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBack = { route = RootRoute.Home.name },
                    )
                } else {
                    Box(Modifier.padding(innerPadding))
                }

            else ->

                HandyGreeting(
                    modifier = Modifier.padding(innerPadding),
                    showDebugEntry = debugBuild,
                    onOpenEnrollment = {
                        returnToSettingsAfterEnrollment = false

                        route = RootRoute.Enrollment.name
                    },
                    onOpenSettings = { route = RootRoute.Settings.name },
                    onOpenDebugVerify = { route = RootRoute.DebugVerify.name },
                    assistant =
                        assistant.takeIf { route == RootRoute.Home.name },
                    showCommandPipelineUi = showCommandPipelineUi,
                )
        }
    }
}

@Composable
private fun HandyGreeting(
    modifier: Modifier = Modifier,
    showDebugEntry: Boolean,
    onOpenEnrollment: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenDebugVerify: () -> Unit = {},
    assistant: HandyAssistantViewModel?,
    showCommandPipelineUi: Boolean,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =

                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.evolwave_logo),
                contentDescription = stringResource(R.string.brand_logo_description),
                modifier =

                    Modifier
                        .fillMaxWidth(0.78f)
                        .padding(bottom = 28.dp),
                contentScale = ContentScale.Fit,
            )

            Text(
                text = stringResource(R.string.main_greeting),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(16.dp))

            OnnxMissingModelsBanner()

            Spacer(Modifier.height(16.dp))

            LaunchedEffect(showCommandPipelineUi, assistant) {
                if (showCommandPipelineUi) {
                    assistant?.noteWakeWordForHeavyModels()
                }
            }

            Button(onClick = onOpenEnrollment) {
                Text(text = stringResource(R.string.open_enrollment))
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(onClick = onOpenSettings) {
                Text(text = stringResource(R.string.open_settings))
            }

            if (showDebugEntry) {
                Spacer(Modifier.height(12.dp))

                TextButton(onClick = onOpenDebugVerify) {
                    Text(text = stringResource(R.string.open_debug_verify))
                }
            }

            if (assistant != null && showCommandPipelineUi) {
                Spacer(Modifier.height(28.dp))

                VoiceCommandDevPanel(vm = assistant)
            } else if (assistant != null) {
                VoiceListeningReleasePanel(vm = assistant)
            }
        }
    }
}

@Composable
private fun VoiceListeningReleasePanel(vm: HandyAssistantViewModel) {
    val context = LocalContext.current

    val toastLine by vm.toastLine.collectAsStateWithLifecycle()

    LaunchedEffect(toastLine) {
        if (!toastLine.isNullOrBlank()) {
            delay(4500)

            vm.consumeToast()
        }
    }

    val micGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    val sherpaBundled =
        remember(context) {
            BundledOnnxBundleHealth.isSherpaListeningPossible(context)
        }

    Spacer(Modifier.height(28.dp))

    Text(
        text = stringResource(R.string.home_listen_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(12.dp))

    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        enabled = micGranted && sherpaBundled,
        onClick = {
            if (!micGranted) {
                vm.notifyAssistantLine(context.getString(R.string.enrollment_missing_mic_perm))

                return@OutlinedButton
            }

            if (!sherpaBundled) {
                vm.notifyAssistantLine(context.getString(R.string.onnx_listen_blocked_no_sherpa))

                return@OutlinedButton
            }

            vm.noteWakeWordForHeavyModels()
        },
    ) {
        Text(stringResource(R.string.home_listen_start))
    }

    toastLine?.takeIf { it.isNotBlank() }?.let { line ->
        Spacer(Modifier.height(12.dp))

        Text(
            text = line,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun VoiceCommandDevPanel(vm: HandyAssistantViewModel) {
    var draft by rememberSaveable { mutableStateOf("") }

    val pending by vm.pendingDestructive.collectAsStateWithLifecycle()

    val toast by vm.toastLine.collectAsStateWithLifecycle()

    val phase by vm.dialogPhase.collectAsStateWithLifecycle()

    LaunchedEffect(toast) {
        if (!toast.isNullOrBlank()) {
            delay(4500)

            vm.consumeToast()
        }
    }

    Text(
        text = stringResource(R.string.command_demo_title),
        style = MaterialTheme.typography.titleMedium,
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.command_demo_phase, phase.toString()),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.command_demo_hint)) },
        singleLine = false,
        minLines = 2,
    )

    Spacer(Modifier.height(12.dp))

    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = { vm.noteWakeWordForHeavyModels() },
    ) {
        Text(stringResource(R.string.command_demo_wake_preload))
    }

    Spacer(Modifier.height(12.dp))

    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { vm.submitRecognizedPhrase(draft) },
    ) {
        Text(stringResource(R.string.command_demo_submit))
    }

    toast?.takeIf { it.isNotBlank() }?.let { line ->

        Spacer(Modifier.height(12.dp))

        Text(
            text = line,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    pending?.let { p ->

        val context = LocalContext.current

        val scope = rememberCoroutineScope()

        val clipRecorder = remember { EnrollmentClipRecorder(context) }

        var recording by remember { mutableStateOf(false) }

        val voiceBusy by vm.voiceConfirmBusy.collectAsStateWithLifecycle()

        DisposableEffect(p) {
            onDispose {
                recording = false

                clipRecorder.stop()
            }
        }

        val clipFile =

            remember {
                File(
                    context.cacheDir,
                    "enrollment/phrase_${DestructiveConfirmVoiceVerifier.CLIP_PHRASE_INDEX}.pcm",
                )
            }

        AlertDialog(
            onDismissRequest = {
                recording = false

                clipRecorder.stop()

                vm.rejectPendingDestructive()
            },
            title = { Text(stringResource(R.string.command_confirm_title)) },
            text = {
                Column {
                    val slotLines =

                        p.slots.entries.joinToString("\n") { (k, v) -> "$k: $v" }

                    Text(
                        text =

                            buildString {
                                append(p.intentId)

                                append("\n\n")

                                append(slotLines)
                            },
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.command_confirm_voice_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (!clipRecorder.hasRecordPermission()) {
                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.command_confirm_need_mic),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    if (recording) {
                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.command_confirm_recording),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            recording = false

                            clipRecorder.stop()

                            vm.rejectPendingDestructive()
                        },
                    ) {
                        Text(stringResource(R.string.command_confirm_cancel))
                    }

                    Spacer(Modifier.weight(1f))

                    if (!recording) {
                        Button(
                            onClick = {
                                if (!clipRecorder.hasRecordPermission()) {
                                    vm.notifyAssistantLine(
                                        context.getString(R.string.command_confirm_need_mic),
                                    )

                                    return@Button
                                }

                                if (!clipRecorder.start(scope, DestructiveConfirmVoiceVerifier.CLIP_PHRASE_INDEX)) {
                                    vm.notifyAssistantLine(
                                        context.getString(R.string.command_confirm_mic_denied),
                                    )

                                    return@Button
                                }

                                recording = true
                            },
                            enabled = !voiceBusy,
                        ) {
                            Text(stringResource(R.string.command_confirm_record_start))
                        }
                    } else {
                        Button(
                            onClick = {
                                clipRecorder.stop()

                                recording = false

                                if (!clipFile.exists() || clipFile.length() < 2L) {
                                    vm.notifyAssistantLine(
                                        context.getString(R.string.command_confirm_no_audio),
                                    )

                                    return@Button
                                }

                                val pcm =

                                    runCatching {
                                        Pcm16LittleEndianIo.readMonoLeShorts(clipFile)
                                    }.getOrElse { e ->

                                        vm.notifyAssistantLine(
                                            e.message

                                                ?: context.getString(R.string.command_confirm_no_audio),
                                        )

                                        return@Button
                                    }

                                vm.submitDestructiveVoiceConfirmFromPcm(pcm)
                            },
                            enabled = !voiceBusy,
                        ) {
                            Text(stringResource(R.string.command_confirm_record_stop_verify))
                        }
                    }
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HandyRootScreenPreview() {
    HandyTheme {
        HandyRootScreen(
            assistant = null,
            showCommandPipelineUi = false,
        )
    }
}
