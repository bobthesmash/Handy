package cz.handy.feature.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cz.handy.core.persistence.ContactAliasStore
import cz.handy.core.persistence.HandyLocalTelemetry
import cz.handy.core.persistence.LocalTelemetryPreferences
import cz.handy.core.persistence.TelemetryLogClearResult
import cz.handy.feature.ui.backup.ProfileBackupCoordinator
import cz.handy.feature.ui.prefs.AssistEnglishNluPreferences
import cz.handy.feature.ui.theme.HandyTheme
import cz.handy.feature.wakeword.WakeWordSensitivityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onOpenEnrollment: () -> Unit,
    onOpenOemHints: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenBetaFeedback: () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        SettingsTopBar(onBack = onBack)
        SettingsBody(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            onOpenEnrollment = onOpenEnrollment,
            onOpenOemHints = onOpenOemHints,
            onOpenPrivacyPolicy = onOpenPrivacyPolicy,
            onOpenBetaFeedback = onOpenBetaFeedback,
        )
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.settings_nav_back))
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun SettingsDiagnosticsBlock() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val telemetryPrefs = remember(context) { LocalTelemetryPreferences(context) }
    val telemetryLog = remember(context) { HandyLocalTelemetry(context.applicationContext, telemetryPrefs) }
    var localTelemetryEnabled by remember { mutableStateOf(telemetryPrefs.isEnabled()) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var exportableLog by remember { mutableStateOf(false) }
    var pendingTelemetryExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(localTelemetryEnabled) {
        exportableLog = withContext(Dispatchers.IO) { telemetryLog.hasExportableStoredLog() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, telemetryLog) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    scope.launch(Dispatchers.IO) {
                        val hasLog = telemetryLog.hasExportableStoredLog()
                        withContext(Dispatchers.Main) {
                            exportableLog = hasLog
                        }
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val createTelemetryDocLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri ->
            val bytes = pendingTelemetryExportBytes
            pendingTelemetryExportBytes = null
            if (activity == null || uri == null || bytes == null) return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                val result =
                    runCatching {
                        activity.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                            ?: error(context.getString(R.string.settings_backup_write_failed))
                    }
                withContext(Dispatchers.Main) {
                    feedbackMessage =
                        result.fold(
                            onSuccess = {
                                context.getString(R.string.settings_local_telemetry_export_ok)
                            },
                            onFailure = { it.message ?: it.toString() },
                        )
                }
            }
        }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.settings_section_diagnostics),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_local_telemetry_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_local_telemetry_title),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Switch(
                checked = localTelemetryEnabled,
                onCheckedChange = { checked ->
                    telemetryPrefs.setEnabled(checked)
                    localTelemetryEnabled = checked
                    feedbackMessage = null
                },
            )
        }
        OutlinedButton(
            onClick = {
                feedbackMessage = null
                scope.launch(Dispatchers.IO) {
                    val bytes = telemetryLog.readStoredLogBytesOrNull()
                    if (bytes == null) {
                        withContext(Dispatchers.Main) {
                            feedbackMessage =
                                context.getString(R.string.settings_local_telemetry_export_nothing)
                            exportableLog = false
                        }
                        return@launch
                    }
                    withContext(Dispatchers.Main) {
                        pendingTelemetryExportBytes = bytes
                        createTelemetryDocLauncher.launch("handy_local_telemetry.ndjson")
                    }
                }
            },
            enabled = exportableLog && activity != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_local_telemetry_export))
        }
        if (!exportableLog) {
            Text(
                text = stringResource(R.string.settings_local_telemetry_export_disabled_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = {
                feedbackMessage = null
                scope.launch(Dispatchers.IO) {
                    val result = telemetryLog.clearStoredLog()
                    val msg =
                        when (result) {
                            TelemetryLogClearResult.WAS_MISSING ->
                                context.getString(R.string.settings_local_telemetry_clear_was_missing)
                            TelemetryLogClearResult.CLEARED ->
                                context.getString(R.string.settings_local_telemetry_clear_ok)
                            TelemetryLogClearResult.FAILED ->
                                context.getString(R.string.settings_local_telemetry_clear_failed)
                        }
                    withContext(Dispatchers.Main) {
                        feedbackMessage = msg
                        exportableLog = telemetryLog.hasExportableStoredLog()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_local_telemetry_clear))
        }
        feedbackMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SettingsBody(
    modifier: Modifier = Modifier,
    onOpenEnrollment: () -> Unit,
    onOpenOemHints: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenBetaFeedback: () -> Unit,
) {
    val context = LocalContext.current
    val wakeStore = remember(context) { WakeWordSensitivityStore(context) }
    val aliasStore = remember(context) { ContactAliasStore(context) }
    var sensitivity by rememberSaveable { mutableFloatStateOf(wakeStore.read()) }

    var aliasKey by remember { mutableStateOf("") }
    var aliasValue by remember { mutableStateOf("") }
    var aliasHint by remember { mutableStateOf<String?>(null) }
    var aliasVersion by remember { mutableStateOf(0) }

    fun bumpAliases() {
        aliasVersion++
    }

    val activity = LocalContext.current as? ComponentActivity
    var backupPwdExport by rememberSaveable { mutableStateOf("") }
    var backupPwdImport by rememberSaveable { mutableStateOf("") }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    val scope = rememberCoroutineScope()
    val backupCoord = remember(context) { ProfileBackupCoordinator(context) }

    val createDocLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri ->
            val bytes = pendingExportBytes
            pendingExportBytes = null
            if (activity == null || uri == null || bytes == null) return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                val result =
                    runCatching {
                        activity.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                            ?: error(context.getString(R.string.settings_backup_write_failed))
                    }
                withContext(Dispatchers.Main) {
                    backupMessage =
                        result.fold(
                            onSuccess = { context.getString(R.string.settings_backup_export_ok) },
                            onFailure = { it.message ?: it.toString() },
                        )
                }
            }
        }

    val openDocLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (activity == null || uri == null) return@rememberLauncherForActivityResult
            val pwd = backupPwdImport.toCharArray()
            scope.launch(Dispatchers.IO) {
                try {
                    val bytes =
                        activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: error(context.getString(R.string.settings_backup_read_failed))
                    backupCoord.importSealedPackage(bytes, pwd).getOrThrow()
                    withContext(Dispatchers.Main) {
                        backupMessage = context.getString(R.string.settings_backup_import_ok)
                        bumpAliases()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        backupMessage = e.message ?: e.toString()
                    }
                } finally {
                    pwd.fill('\u0000')
                }
            }
        }

    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.settings_section_voice),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_enrollment_hint),
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            onClick = onOpenEnrollment,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_open_enrollment))
        }
        Text(
            text = stringResource(R.string.settings_reenroll_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.settings_section_wake),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text =
                stringResource(
                    R.string.settings_wake_sensitivity_value,
                    (sensitivity * 100f).roundToInt(),
                ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = sensitivity,
            onValueChange = { sensitivity = it },
            valueRange = 0f..1f,
            onValueChangeFinished = { wakeStore.write(sensitivity) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.settings_wake_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.settings_section_oem),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_oem_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onOpenOemHints,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_open_oem_hints))
        }

        Text(
            text = stringResource(R.string.settings_section_aliases),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_aliases_hint),
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = aliasKey,
            onValueChange = {
                aliasKey = it
                aliasHint = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_alias_spoken)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = aliasValue,
            onValueChange = {
                aliasValue = it
                aliasHint = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_alias_target)) },
            singleLine = true,
        )
        aliasHint?.let { h ->
            Text(
                text = h,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(
            onClick = {
                aliasStore.upsert(aliasKey, aliasValue).fold(
                    onSuccess = {
                        aliasKey = ""
                        aliasValue = ""
                        aliasHint = null
                        bumpAliases()
                    },
                    onFailure = { e -> aliasHint = e.message ?: e.toString() },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_alias_add))
        }

        key(aliasVersion) {
            val list = remember(aliasVersion) { aliasStore.allAliasesSorted() }
            if (list.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_aliases_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                list.forEach { (k, v) ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(k, style = MaterialTheme.typography.titleSmall)
                                Text(v, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(
                                onClick = {
                                    aliasStore.remove(k)
                                    bumpAliases()
                                },
                            ) {
                                Text(stringResource(R.string.settings_alias_remove))
                            }
                        }
                    }
                }
            }
        }

        activity?.let {
            Text(
                text = stringResource(R.string.settings_section_backup),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_backup_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = backupPwdExport,
                onValueChange = {
                    backupPwdExport = it
                    backupMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_backup_password_export)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            OutlinedButton(
                onClick = {
                    if (backupPwdExport.length < 8) {
                        backupMessage = context.getString(R.string.settings_backup_pwd_short)
                        return@OutlinedButton
                    }
                    backupMessage = null
                    scope.launch(Dispatchers.IO) {
                        val pwd = backupPwdExport.toCharArray()
                        try {
                            val pack = backupCoord.exportSealedPackage(pwd).getOrThrow()
                            withContext(Dispatchers.Main) {
                                pendingExportBytes = pack
                                createDocLauncher.launch("handy-profile.handy")
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                backupMessage = e.message ?: e.toString()
                            }
                        } finally {
                            pwd.fill('\u0000')
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_backup_export))
            }
            OutlinedTextField(
                value = backupPwdImport,
                onValueChange = {
                    backupPwdImport = it
                    backupMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_backup_password_import)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            OutlinedButton(
                onClick = {
                    if (backupPwdImport.isBlank()) {
                        backupMessage = context.getString(R.string.settings_backup_pwd_required)
                        return@OutlinedButton
                    }
                    backupMessage = null
                    openDocLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_backup_import))
            }
            backupMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Text(
            text = stringResource(R.string.settings_section_privacy),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_privacy_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onOpenPrivacyPolicy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_open_privacy_policy))
        }

        SettingsF5ExperimentalBlock()

        SettingsDiagnosticsBlock()

        Text(
            text = stringResource(R.string.settings_section_beta_feedback),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_beta_feedback_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onOpenBetaFeedback,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_open_beta_feedback))
        }

        AppVersionFooterText(spacerBeforeWhenPresent = 28.dp)
    }
}

@Composable
private fun SettingsF5ExperimentalBlock() {
    val context = LocalContext.current
    val enPrefs = remember(context) { AssistEnglishNluPreferences(context) }
    var englishOverlay by rememberSaveable { mutableStateOf(enPrefs.isEnabled()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_section_f5),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_f5_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_f5_en_overlay_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.settings_f5_en_overlay_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = englishOverlay,
                onCheckedChange = {
                    englishOverlay = it
                    enPrefs.setEnabled(it)
                },
            )
        }
        Text(
            text = stringResource(R.string.settings_f5_piper_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_f5_open_accessibility))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    HandyTheme {
        Surface {
            SettingsScreen(
                onBack = {},
                onOpenEnrollment = {},
                onOpenOemHints = {},
                onOpenPrivacyPolicy = {},
                onOpenBetaFeedback = {},
            )
        }
    }
}
