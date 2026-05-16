package cz.handy.feature.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.handy.core.persistence.entity.BetaFeedbackEntity
import cz.handy.feature.ui.feedback.BetaFeedbackViewModel
import cz.handy.feature.ui.theme.HandyTheme
import cz.handy.feature.voiceid.enrollment.EnrollmentClipRecorder
import cz.handy.feature.voiceid.io.Pcm16LittleEndianIo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

private const val BETA_FEEDBACK_CLIP_INDEX = 881

private const val MIN_FEEDBACK_STAR = 1

private const val MAX_FEEDBACK_STAR = 5

private const val STAR_SLIDER_STEPS = 3

private const val MIN_PCM_CLIP_BYTES = 512L

private const val FEEDBACK_MESSAGE_PREVIEW_LINES = 5

private const val FEEDBACK_BANNER_VM_ERROR_HIDE_MS = 6000L

private const val FEEDBACK_BANNER_CLIP_TOO_SHORT_HIDE_MS = 5000L

private const val FEEDBACK_BANNER_TRANSCRIPT_OK_HIDE_MS = 4000L

private const val FEEDBACK_BANNER_ASR_EMPTY_HIDE_MS = 5000L

private const val FEEDBACK_BANNER_PCM_DECODE_FAIL_HIDE_MS = 6000L

private const val FEEDBACK_BANNER_VALIDATION_HIDE_MS = 5500L

private const val FEEDBACK_BANNER_SAVED_HIDE_MS = 4200L

private const val FEEDBACK_BANNER_SHARE_HIDE_MS = 5000L

@Composable
fun BetaFeedbackScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: BetaFeedbackViewModel =
        viewModel(factory = BetaFeedbackViewModel.Factory(app))

    Column(modifier.fillMaxWidth()) {
        FeedbackTopBar(onBack = onBack)
        FeedbackBody(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            vm = vm,
        )
    }
}

@Composable
private fun FeedbackTopBar(onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.beta_feedback_nav_back))
            }
            Text(
                text = stringResource(R.string.beta_feedback_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun FeedbackBody(
    modifier: Modifier = Modifier,
    vm: BetaFeedbackViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stars by rememberSaveable { mutableIntStateOf(4) }
    var draft by rememberSaveable { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var localBanner by remember { mutableStateOf<String?>(null) }
    var sharePayload by remember { mutableStateOf<String?>(null) }

    val clipRecorder = remember { EnrollmentClipRecorder(context) }
    val clipFile =
        remember(context) {
            File(File(context.cacheDir, "enrollment"), "phrase_$BETA_FEEDBACK_CLIP_INDEX.pcm")
        }

    val permLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (!granted) {
                localBanner = context.getString(R.string.beta_feedback_need_mic_after_deny)
            }
        }

    LaunchedEffect(sharePayload) {
        val text = sharePayload ?: return@LaunchedEffect
        sharePayload = null
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.beta_feedback_share_subject))
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            }
        runCatching {
            context.startActivity(
                Intent.createChooser(
                    send,
                    context.getString(R.string.beta_feedback_share_chooser_title),
                ),
            )
        }.onFailure { _ ->
            localBanner = context.getString(R.string.beta_feedback_share_failed)
        }
    }

    val vmError by vm.statusLine.collectAsStateWithLifecycle()

    val transcribeBusy by vm.transcriptionBusy.collectAsStateWithLifecycle()

    val recentSaved by vm.recentSaved.collectAsStateWithLifecycle()

    val dateFmt =
        remember {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        }

    val starsSliderContentDescription =
        stringResource(R.string.beta_feedback_stars, stars)
    val recordStartContentDescription =
        stringResource(R.string.beta_feedback_record_start)
    val recordStopContentDescription =
        stringResource(R.string.beta_feedback_record_stop)

    LaunchedEffect(vmError) {
        if (!vmError.isNullOrBlank()) {
            delay(FEEDBACK_BANNER_VM_ERROR_HIDE_MS)
            vm.consumeStatus()
        }
    }

    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.beta_feedback_intro), style = MaterialTheme.typography.bodyMedium)

        Text(
            stringResource(R.string.beta_feedback_stars, stars),
            style = MaterialTheme.typography.bodyMedium,
        )

        Slider(
            value = stars.toFloat().coerceIn(MIN_FEEDBACK_STAR.toFloat(), MAX_FEEDBACK_STAR.toFloat()),
            valueRange = MIN_FEEDBACK_STAR.toFloat()..MAX_FEEDBACK_STAR.toFloat(),
            steps = STAR_SLIDER_STEPS,
            onValueChange = { stars = it.roundToInt().coerceIn(MIN_FEEDBACK_STAR, MAX_FEEDBACK_STAR) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = starsSliderContentDescription
                    },
        )

        Text(
            stringResource(R.string.beta_feedback_voice_block_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!recording) {
                Button(
                    onClick = {
                        localBanner = null
                        if (!clipRecorder.hasRecordPermission()) {
                            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            localBanner = context.getString(R.string.beta_feedback_need_mic)
                            return@Button
                        }
                        if (!clipRecorder.start(scope, BETA_FEEDBACK_CLIP_INDEX)) {
                            localBanner = context.getString(R.string.beta_feedback_mic_denied)
                            return@Button
                        }
                        recording = true
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = recordStartContentDescription
                            },
                    enabled = !transcribeBusy,
                ) {
                    Text(stringResource(R.string.beta_feedback_record_start))
                }
            } else {
                Button(
                    onClick = {
                        clipRecorder.stop()
                        recording = false
                        scope.launch {
                            if (!clipFile.exists() || clipFile.length() < MIN_PCM_CLIP_BYTES) {
                                localBanner = context.getString(R.string.beta_feedback_no_audio_short)
                                delay(FEEDBACK_BANNER_CLIP_TOO_SHORT_HIDE_MS)
                                localBanner = null
                                return@launch
                            }
                            runCatching {
                                val pcm = Pcm16LittleEndianIo.readMonoLeShorts(clipFile)
                                vm.transcribeLocally(pcm)
                            }.fold(
                                onSuccess = { line ->
                                    if (line.isNotBlank()) {
                                        draft += (if (draft.isBlank()) "" else "\n") + line.trim()
                                        localBanner = context.getString(R.string.beta_feedback_transcript_appended)
                                        delay(FEEDBACK_BANNER_TRANSCRIPT_OK_HIDE_MS)
                                        localBanner = null
                                    } else {
                                        localBanner = context.getString(R.string.beta_feedback_asr_empty)
                                        delay(FEEDBACK_BANNER_ASR_EMPTY_HIDE_MS)
                                        localBanner = null
                                    }
                                },
                                onFailure = { _ ->
                                    localBanner = context.getString(R.string.beta_feedback_no_audio_or_read)
                                    delay(FEEDBACK_BANNER_PCM_DECODE_FAIL_HIDE_MS)
                                    localBanner = null
                                },
                            )
                        }
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = recordStopContentDescription
                            },
                    enabled = !transcribeBusy,
                ) {
                    Text(stringResource(R.string.beta_feedback_record_stop))
                }
            }
        }

        if (recording || transcribeBusy) {
            Text(
                text =
                    stringResource(
                        if (recording) R.string.beta_feedback_recording else R.string.beta_feedback_transcribing,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Text(
            stringResource(R.string.beta_feedback_message_label),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = draft,
            onValueChange = {
                draft = it
                localBanner = null
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            placeholder = {
                Text(stringResource(R.string.beta_feedback_message_placeholder))
            },
        )

        Button(
            onClick = {
                val body = draft.trim()
                if (body.isBlank()) {
                    scope.launch {
                        localBanner = context.getString(R.string.beta_feedback_need_body)
                        delay(FEEDBACK_BANNER_VALIDATION_HIDE_MS)
                        localBanner = null
                    }
                    return@Button
                }
                vm.saveFeedback(
                    stars = stars,
                    message = body,
                    onSuccess = {
                        draft = ""
                        scope.launch {
                            localBanner = context.getString(R.string.beta_feedback_saved_room)
                            delay(FEEDBACK_BANNER_SAVED_HIDE_MS)
                            localBanner = null
                        }
                    },
                    onFailure = {},
                )
            },
            Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.beta_feedback_save_local))
        }

        OutlinedButton(
            onClick = {
                val body = draft.trim()
                if (body.isBlank()) {
                    scope.launch {
                        localBanner = context.getString(R.string.beta_feedback_need_body_for_share)
                        delay(FEEDBACK_BANNER_VALIDATION_HIDE_MS)
                        localBanner = null
                    }
                    return@OutlinedButton
                }
                val pack =
                    buildString {
                        append(context.getString(R.string.beta_feedback_share_subject))
                        append("\n\n")
                        append(context.getString(R.string.beta_feedback_share_stars_line, stars))
                        append("\n\n")
                        append(body)
                        installedAppFooterLine(context.applicationContext)?.let { buildLine ->
                            append("\n\n")
                            append(buildLine)
                        }
                        append("\n\n")
                        append(context.getString(R.string.beta_feedback_share_footer))
                    }
                sharePayload = pack
                scope.launch {
                    localBanner = context.getString(R.string.beta_feedback_share_instructions)
                    delay(FEEDBACK_BANNER_SHARE_HIDE_MS)
                    localBanner = null
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.beta_feedback_share_outline))
        }

        vmError?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        localBanner?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
        }

        SavedFeedbackRecentBlock(
            rows = recentSaved,
            dateFmt = dateFmt,
            maxListed = BetaFeedbackViewModel.RECENT_FEEDBACK_QUERY_LIMIT,
        )

        AppVersionFooterText(spacerBeforeWhenPresent = 16.dp)

        Text(
            stringResource(R.string.beta_feedback_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun SavedFeedbackRecentBlock(
    rows: List<BetaFeedbackEntity>,
    dateFmt: DateFormat,
    maxListed: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.beta_feedback_recent_heading, maxListed),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (rows.isEmpty()) {
            Text(
                stringResource(R.string.beta_feedback_recent_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        } else {
            rows.forEach { row ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            stringResource(
                                R.string.beta_feedback_recent_meta,
                                row.satisfactionStars,
                                dateFmt.format(Date(row.createdAtEpochMillis)),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            row.messageText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = FEEDBACK_MESSAGE_PREVIEW_LINES,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedbackTopPreview() {
    HandyTheme {
        FeedbackTopBar(onBack = {})
    }
}
