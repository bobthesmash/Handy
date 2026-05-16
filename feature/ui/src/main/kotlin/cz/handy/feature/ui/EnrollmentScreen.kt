package cz.handy.feature.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cz.handy.feature.ui.theme.HandyTheme
import cz.handy.feature.voiceid.enrollment.EnrollmentClipRecorder
import cz.handy.feature.voiceid.enrollment.EnrollmentProfileFinalizer
import kotlinx.coroutines.launch

private val enrollmentPhraseIds =
    listOf(
        R.string.enrollment_phrase_1,
        R.string.enrollment_phrase_2,
        R.string.enrollment_phrase_3,
        R.string.enrollment_phrase_4,
        R.string.enrollment_phrase_5,
        R.string.enrollment_phrase_6,
        R.string.enrollment_phrase_7,
        R.string.enrollment_phrase_8,
    )

/** Seznam 5–8 vět pro zápis hlasu ([F1-T01]). Počet řídí [maxCount]. */
@Composable
fun rememberEnrollmentPhraseStrings(maxCount: Int = 6): List<String> {
    val ctx = LocalContext.current
    val configuration = LocalConfiguration.current
    require(maxCount in 5..enrollmentPhraseIds.size)
    val take = enrollmentPhraseIds.take(maxCount.coerceAtMost(enrollmentPhraseIds.size))
    return remember(maxCount, configuration) {
        take.map { id -> ctx.getString(id) }
    }
}

@Composable
fun EnrollmentScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        EnrollmentTopBar(onBack = onBack)
        EnrollmentBody(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            phrases = rememberEnrollmentPhraseStrings(maxCount = 6),
        )
    }
}

@Composable
private fun EnrollmentTopBar(onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.enrollment_nav_back))
            }
            Text(
                text = stringResource(R.string.enrollment_title),
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** [recorder] null = vytvoří [EnrollmentClipRecorder] s application contextem. */
@Composable
fun EnrollmentBody(
    phrases: List<String>,
    modifier: Modifier = Modifier,
    recorder: EnrollmentClipRecorder? = null,
    onRecordedCountChange: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeRecorder =
        remember(recorder) {
            recorder ?: EnrollmentClipRecorder(context.applicationContext)
        }

    DisposableEffect(activeRecorder) {
        onDispose { activeRecorder.stop() }
    }

    val level by activeRecorder.level.collectAsState()

    var activeIndex by rememberSaveable { mutableIntStateOf(-1) }
    var recordedMask by rememberSaveable { mutableStateOf(0) }

    val micOk =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    fun bumpRecorded(bit: Int) {
        recordedMask = recordedMask or (1 shl bit)
        val n = phrases.size
        val cnt = (0 until n).count { recordedMask and (1 shl it) != 0 }
        onRecordedCountChange(cnt)
    }

    val allRecorded =
        phrases.indices.all { ix -> recordedMask and (1 shl ix) != 0 }

    var saveBusy by remember { mutableStateOf(false) }

    var saveBanner by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.enrollment_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!micOk) {
            Text(
                text = stringResource(R.string.enrollment_missing_mic_perm),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        phrases.forEachIndexed { index, phrase ->
            PhraseEnrollmentCard(
                indexDisplay = index + 1,
                phraseText = phrase,
                isRecording = activeIndex == index,
                isDone = recordedMask and (1 shl index) != 0,
                meterLevel = if (activeIndex == index) level else 0f,
                onRecord = {
                    if (!activeRecorder.hasRecordPermission()) return@PhraseEnrollmentCard
                    val ok =
                        activeRecorder.start(
                            scope = scope,
                            phraseIndex = index,
                        )
                    if (ok) activeIndex = index
                },
                onStop = {
                    activeRecorder.stop()
                    if (activeIndex == index) {
                        bumpRecorded(index)
                    }
                    activeIndex = -1
                },
            )
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = micOk && allRecorded && !saveBusy,
            onClick = {
                saveBanner = null

                saveBusy = true

                scope.launch {
                    val outcome =
                        EnrollmentProfileFinalizer(context.applicationContext)
                            .finalizeEnrollmentClips(phrases.size)

                    saveBusy = false

                    saveBanner =
                        outcome.fold(
                            onSuccess = {
                                val msg = context.getString(R.string.enrollment_save_success)
                                true to msg
                            },
                            onFailure = { e ->
                                val suffix =
                                    e.message?.takeIf { it.isNotBlank() }
                                        ?: e::class.simpleName.orEmpty()
                                false to (
                                    context.getString(R.string.enrollment_save_error_prefix) +
                                        ": $suffix"
                                    )
                            },
                        )
                }
            },
        ) {
            Text(
                text =
                    if (saveBusy) {
                        stringResource(R.string.enrollment_save_busy)
                    } else {
                        stringResource(R.string.enrollment_action_save_profile)
                    },
            )
        }

        saveBanner?.let { (ok, msg) ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RecordingPulseDot(isRecording: Boolean) {
    if (!isRecording) return
    val transition =
        rememberInfiniteTransition(
            label = "recPulse",
        )
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 640,
                        easing = LinearEasing,
                    ),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "recPulseAlpha",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.enrollment_pulse_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .alpha(alpha)
                    .background(MaterialTheme.colorScheme.error, CircleShape),
        )
    }
}

@Composable
private fun PhraseEnrollmentCard(
    indexDisplay: Int,
    phraseText: String,
    isRecording: Boolean,
    isDone: Boolean,
    meterLevel: Float,
    onRecord: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.enrollment_phrase_heading, indexDisplay),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(text = phraseText, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            RecordingPulseDot(isRecording = isRecording)
            Spacer(Modifier.height(10.dp))
            if (isRecording) {
                LinearProgressIndicator(
                    progress = { meterLevel },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRecord,
                    enabled = !isRecording,
                ) {
                    Text(stringResource(R.string.enrollment_action_record))
                }
                Button(
                    onClick = onStop,
                    enabled = isRecording,
                ) {
                    Text(stringResource(R.string.enrollment_action_stop))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text =
                    stringResource(
                        if (isDone) {
                            R.string.enrollment_saved_hint
                        } else {
                            R.string.enrollment_pending_hint
                        },
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PhraseEnrollmentCardPreview() {
    HandyTheme {
        PhraseEnrollmentCard(
            indexDisplay = 1,
            phraseText = "Ukázková věta pro vizuální kontrolu karty zápisu hlasu.",
            isRecording = true,
            isDone = false,
            meterLevel = 0.55f,
            onRecord = {},
            onStop = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
