package cz.handy.feature.voiceid.enrollment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import cz.handy.core.audio.MicCaptureConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Short phrase capture for voice enrollment UI; writes raw little-endian PCM16 mono to cache ([F1-T01]).
 * ECAPA embedding lands in [F1-T02]; this class only gathers clips + live level.
 */
class EnrollmentClipRecorder(
    context: Context,
) {
    private val appContext = context.applicationContext

    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    private var record: AudioRecord? = null
    private var job: Job? = null

    fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Starts capture for [phraseIndex]. Stops any previous session.
     * Returns false if permission missing or [AudioRecord] cannot open.
     */
    @Suppress("MissingPermission")
    fun start(
        scope: CoroutineScope,
        phraseIndex: Int,
    ): Boolean {
        if (!hasRecordPermission()) return false
        stop()

        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val channel = AudioFormat.CHANNEL_IN_MONO
        val minBuf =
            AudioRecord.getMinBufferSize(
                MicCaptureConfig.SAMPLE_RATE_HZ,
                channel,
                encoding,
            )
        if (minBuf <= AudioRecord.ERROR_BAD_VALUE || minBuf <= 0) return false

        val rec =
            try {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    MicCaptureConfig.SAMPLE_RATE_HZ,
                    channel,
                    encoding,
                    minBuf.coerceAtLeast(minBuf * 2),
                )
            } catch (_: IllegalArgumentException) {
                return false
            }

        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return false
        }

        record = rec
        rec.startRecording()

        job =
            scope.launch(Dispatchers.IO) {
                val batch = ShortArray(1024)
                val outFile = cachedPhraseFile(appContext, phraseIndex)
                FileOutputStream(outFile).use { fos ->
                    var ema = 0f
                    while (isActive) {
                        val read = rec.read(batch, 0, batch.size)
                        if (read <= 0) break
                        val raw =
                            PcmIntensity.rmsNormalized01(
                                batch,
                                0,
                                read,
                            )
                        ema = ema * LEVEL_EMA_PREV + raw * LEVEL_EMA_NEW
                        _level.value = ema
                        for (i in 0 until read) {
                            val s = batch[i]
                            fos.write(s.toInt() and 0xff)
                            fos.write(s.toInt() shr 8 and 0xff)
                        }
                    }
                }
            }

        return true
    }

    companion object {
        private const val LEVEL_EMA_PREV = 0.75f
        private const val LEVEL_EMA_NEW = 0.25f

        const val ENROLLMENT_CACHE_SUBDIR_NAME = "enrollment"

        fun cachedPhraseFile(
            context: Context,
            phraseIndex: Int,
        ): File {
            val outDir =
                File(context.applicationContext.cacheDir, ENROLLMENT_CACHE_SUBDIR_NAME).apply {
                    mkdirs()
                }
            return File(outDir, "phrase_$phraseIndex.pcm")
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        val r = record
        record = null
        _level.value = 0f
        if (r == null) return
        try {
            if (r.state == AudioRecord.STATE_INITIALIZED) {
                r.stop()
            }
        } catch (_: IllegalStateException) {
            // ignore
        }
        try {
            r.release()
        } catch (_: IllegalStateException) {
            // ignore
        }
    }
}
