package cz.handy.core.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Foreground service that captures microphone PCM at 16 kHz mono into a 3-second pre-roll ring buffer.
 * Vstup prochází [MonoSpeechProgrammeGainNormalizer] (−23 LUFS proxy) před zápisem ([F2-T10]).
 * Volitelně [MicHardwareAudioEffects] (NS/AEC) na audio session ([F2-T11]).
 */
class EarService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var captureJob: Job? = null

    /**
     * Bluetooth SCO + [MODE_IN_COMMUNICATION] — inicializace až v [onCreate]; v konstruktoru služby
     * ještě není připojený [Context] (viz crash při `getApplicationContext()`).
     */
    private lateinit var audioRouting: AudioHandsFreeRouting

    @Volatile
    private var audioRecord: AudioRecord? = null

    /** For later pipeline stages; thread-safe via [MonoPcmRingBuffer]. */
    val ringBuffer = MonoPcmRingBuffer(MicCaptureConfig.ringCapacitySamples)

    /** Úroveň vstupu směrem k −23 LUFS (RMS proxy) před zápisem do bufferu ([F2-T10]). */
    private val inputGainNormalizer = MonoSpeechProgrammeGainNormalizer()

    private var micHardwareEffects: MicHardwareAudioEffects? = null

    private var foregroundUiState: EarForegroundUiState = EarForegroundUiState.Idle

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioRouting = AudioHandsFreeRouting(applicationContext)
        EarAudioBridge.attach(ringBuffer)
        createNotificationChannel()
    }

    override fun onDestroy() {
        EarAudioBridge.detach(ringBuffer)
        captureJob?.cancel()
        captureJob = null
        releaseRecorder()
        if (::audioRouting.isInitialized) {
            audioRouting.release()
        }
        super.onDestroy()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP) {
            stopCaptureAndTeardown()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_UPDATE_FOREGROUND_UI_STATE) {
            val name = intent.getStringExtra(EXTRA_FOREGROUND_UI_STATE)
            foregroundUiState =
                runCatching { name?.let { EarForegroundUiState.valueOf(it) } }
                    .getOrNull() ?: EarForegroundUiState.Idle
        }

        val notification = buildNotification(foregroundUiState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }

        if (captureJob?.isActive != true) {
            captureJob =
                scope.launch(Dispatchers.IO) {
                    runCapture()
                }
        }
        return START_STICKY
    }

    private fun stopCaptureAndTeardown() {
        releaseRecorder()
        captureJob?.cancel()
        captureJob = null
        if (::audioRouting.isInitialized) {
            audioRouting.endHandsFreeMicRoute()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.ear_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun buildNotification(state: EarForegroundUiState): Notification {
        val titleRes =
            when (state) {
                EarForegroundUiState.Idle -> R.string.ear_notification_idle_title
                EarForegroundUiState.Listening ->
                    R.string.ear_notification_listening_title
                EarForegroundUiState.Processing ->
                    R.string.ear_notification_processing_title
            }
        val textRes =
            when (state) {
                EarForegroundUiState.Idle -> R.string.ear_notification_idle_text
                EarForegroundUiState.Listening ->
                    R.string.ear_notification_listening_text
                EarForegroundUiState.Processing ->
                    R.string.ear_notification_processing_text
            }
        return NotificationCompat
            .Builder(
                this,
                CHANNEL_ID,
            ).setContentTitle(getString(titleRes))
            .setContentText(getString(textRes))
            .setSmallIcon(R.drawable.ic_ear_small)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(
                0,
                getString(R.string.ear_notification_stop),
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, EarService::class.java).setAction(ACTION_STOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            ).build()
    }

    private suspend fun runCapture() {
        audioRouting.beginHandsFreeMicRoute()
        try {
            val record =
                createAudioRecord() ?: run {
                    shutdownAfterInitFailure()
                    return
                }

            audioRecord = record
            record.startRecording()
            micHardwareEffects = MicHardwareAudioEffects.tryAttach(record.audioSessionId)
            val batch = ShortArray(CAPTURE_READ_SHORTS)

            try {
                while (coroutineContext.isActive) {
                    val read = record.read(batch, 0, batch.size)
                    if (read == AudioRecord.ERROR_INVALID_OPERATION ||
                        read == AudioRecord.ERROR_BAD_VALUE
                    ) {
                        break
                    }
                    if (read > 0) {
                        inputGainNormalizer.applyInPlace(batch, read)
                        ringBuffer.write(batch, 0, read)
                    }
                }
            } finally {
                releaseRecordingResources(record)
            }
        } finally {
            audioRouting.endHandsFreeMicRoute()
        }
    }

    private fun shutdownAfterInitFailure() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseRecordingResources(record: AudioRecord) {
        releaseMicHardwareEffects()
        safelyStopAndRelease(record)
        if (audioRecord === record) {
            audioRecord = null
        }
    }

    private fun releaseMicHardwareEffects() {
        micHardwareEffects?.release()
        micHardwareEffects = null
    }

    private fun createAudioRecord(): AudioRecord? {
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val channel = AudioFormat.CHANNEL_IN_MONO
        val minBuf =
            AudioRecord.getMinBufferSize(
                MicCaptureConfig.SAMPLE_RATE_HZ,
                channel,
                encoding,
            )
        if (minBuf <= AudioRecord.ERROR_BAD_VALUE || minBuf <= 0) {
            return null
        }

        val source = audioRouting.preferredAudioSource()

        return try {
            @Suppress("MissingPermission")
            AudioRecord(
                source,
                MicCaptureConfig.SAMPLE_RATE_HZ,
                channel,
                encoding,
                minBuf.coerceAtLeast(minBuf * 2),
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun releaseRecorder() {
        val rec = audioRecord ?: return
        releaseMicHardwareEffects()
        safelyStopAndRelease(rec)
        audioRecord = null
    }

    private fun safelyStopAndRelease(record: AudioRecord) {
        try {
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                record.stop()
            }
        } catch (_: IllegalStateException) {
            // already stopped / invalid state — safe to proceed
        }
        try {
            record.release()
        } catch (_: IllegalStateException) {
            // tolerate double-teardown races with the capture coroutine [F0-T04].
        }
    }

    companion object {
        private const val TAG = "HandyEarService"
        private const val CHANNEL_ID = "ear_listening_channel"
        private const val ACTION_STOP = "cz.handy.core.audio.ACTION_STOP_LISTENING"
        private const val ACTION_UPDATE_FOREGROUND_UI_STATE =
            "cz.handy.core.audio.ACTION_UPDATE_FOREGROUND_UI_STATE"
        private const val EXTRA_FOREGROUND_UI_STATE = "foreground_ui_state"
        private const val NOTIFICATION_ID = 7134
        private const val CAPTURE_READ_SHORTS = 2048

        /**
         * Spustí foreground službu; při odmítnutí systémem (oprávnění, OEM) nepropaguje výjimku do UI.
         *
         * @return `true` pokud byl intent odeslán bez výjimky
         */
        fun start(context: Context): Boolean =
            runForegroundServiceIntent(
                context,
                Intent(context.applicationContext, EarService::class.java),
            )

        /**
         * Aktualizuje titulek a text notifikace podle fáze dialogu ([F1-T20]).
         * Volitelné jen když má služba běžet (jinak by intent znovu nastartoval FGS).
         */
        fun notifyForegroundUiState(
            context: Context,
            state: EarForegroundUiState,
        ): Boolean =
            runForegroundServiceIntent(
                context,
                Intent(context.applicationContext, EarService::class.java).apply {
                    action = ACTION_UPDATE_FOREGROUND_UI_STATE
                    putExtra(EXTRA_FOREGROUND_UI_STATE, state.name)
                },
            )

        private fun runForegroundServiceIntent(
            context: Context,
            intent: Intent,
        ): Boolean =
            runCatching {
                context.applicationContext.startForegroundService(intent)
            }.onFailure { e ->
                Log.w(TAG, "Foreground service intent rejected: ${e.javaClass.simpleName}", e)
            }.isSuccess

        fun stop(context: Context) {
            val app = context.applicationContext
            app.startService(
                Intent(app, EarService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
