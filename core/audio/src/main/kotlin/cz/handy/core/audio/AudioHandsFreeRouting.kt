package cz.handy.core.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlin.math.min

/**
 * Prepares [AudioManager] for hands-free / Bluetooth SCO capture ([F0-T06]).
 * Při výpadku BT SCO během session znovu navazuje smyčku s exponenciálním backoffem ([F3-T07]).
 */
class AudioHandsFreeRouting(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handlerThread =
        HandlerThread(TAG).also {
            it.start()
        }
    private val handler = Handler(handlerThread.looper)

    @Volatile
    private var routeSessionActive = false

    private var callbackRegistered = false

    private var scoReconnectSteps = 0

    private val scoReconnectRunnable =
        object : Runnable {
            override fun run() {
                if (!routeSessionActive) return
                applyBluetoothCommunicationRoute()
                if (scoReconnectSteps >= MAX_SCO_RECONNECT_STEPS) {
                    Log.w(TAG, "SCO reconnect: max steps ($MAX_SCO_RECONNECT_STEPS)")
                    return
                }
                scoReconnectSteps++
                val delayMs =
                    min(
                        MAX_SCO_RECONNECT_DELAY_MS,
                        INITIAL_SCO_RECONNECT_DELAY_MS shl min(scoReconnectSteps, 6),
                    )
                handler.postDelayed(this, delayMs)
            }
        }

    private val deviceCallback =
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                for (device in addedDevices) {
                    Log.d(TAG, "device added id=${device.id} type=${device.type}")
                    if (!routeSessionActive) continue
                    if (device.type != AudioDeviceInfo.TYPE_BLUETOOTH_SCO) continue
                    if (!mayUseBluetoothRouting()) continue
                    Log.d(TAG, "Bluetooth SCO added — refresh route")
                    cancelScoReconnectChain()
                    applyBluetoothCommunicationRoute()
                }
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                for (device in removedDevices) {
                    Log.d(TAG, "device removed id=${device.id} type=${device.type}")
                    if (!routeSessionActive) continue
                    if (device.type != AudioDeviceInfo.TYPE_BLUETOOTH_SCO) continue
                    if (!mayUseBluetoothRouting()) continue
                    Log.w(TAG, "Bluetooth SCO removed — reconnect chain")
                    scheduleScoReconnectChain()
                }
            }
        }

    fun preferredAudioSource(): Int =
        if (routeSessionActive) {
            MediaRecorder.AudioSource.VOICE_COMMUNICATION
        } else {
            MediaRecorder.AudioSource.DEFAULT
        }

    fun beginHandsFreeMicRoute() {
        if (routeSessionActive) return
        routeSessionActive = true
        cancelScoReconnectChain()
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        if (!mayUseBluetoothRouting()) {
            Log.d(TAG, "BLUETOOTH_CONNECT not granted — using built-in routing only")
            tryRegisterCallback()
            return
        }

        applyBluetoothCommunicationRoute()
        tryRegisterCallback()
    }

    private fun mayUseBluetoothRouting(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return hasBluetoothConnectPermission()
    }

    private fun applyBluetoothCommunicationRoute() {
        if (!routeSessionActive) return
        if (!mayUseBluetoothRouting()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            routeApi31Bluetooth()
        } else {
            @Suppress("DEPRECATION")
            audioManager.startBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = true
        }
    }

    private fun scheduleScoReconnectChain() {
        if (!routeSessionActive) return
        cancelScoReconnectChain()
        handler.postDelayed(scoReconnectRunnable, INITIAL_SCO_RECONNECT_DELAY_MS)
    }

    private fun cancelScoReconnectChain() {
        handler.removeCallbacks(scoReconnectRunnable)
        scoReconnectSteps = 0
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun routeApi31Bluetooth() {
        val devices = audioManager.availableCommunicationDevices
        val sco =
            devices.find { device ->
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }
        if (sco != null) {
            val ok = audioManager.setCommunicationDevice(sco)
            if (!ok) {
                Log.w(TAG, "setCommunicationDevice rejected for device id=${sco.id}")
            }
        } else {
            Log.d(TAG, "No bluetooth communication device advertised — relying on MODE_IN_COMMUNICATION")
            @Suppress("DEPRECATION")
            audioManager.startBluetoothSco()
        }
    }

    private fun tryRegisterCallback() {
        if (callbackRegistered) return
        try {
            audioManager.registerAudioDeviceCallback(deviceCallback, handler)
            callbackRegistered = true
        } catch (_: SecurityException) {
            Log.w(TAG, "registerAudioDeviceCallback failed — missing audio routing permission")
        }
    }

    fun endHandsFreeMicRoute() {
        if (!routeSessionActive) return
        routeSessionActive = false
        cancelScoReconnectChain()
        unregisterCallbackQuietly()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hasBluetoothConnectPermission()) {
            audioManager.clearCommunicationDevice()
        }
        @Suppress("DEPRECATION")
        audioManager.stopBluetoothSco()
        @Suppress("DEPRECATION")
        audioManager.isBluetoothScoOn = false

        audioManager.mode = AudioManager.MODE_NORMAL
    }

    fun release() {
        endHandsFreeMicRoute()
        handlerThread.quitSafely()
    }

    private fun unregisterCallbackQuietly() {
        if (!callbackRegistered) return
        try {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
        } catch (_: IllegalArgumentException) {
            // already unregistered
        }
        callbackRegistered = false
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        private const val TAG = "HandyAudioRoute"
        private const val INITIAL_SCO_RECONNECT_DELAY_MS = 400L
        private const val MAX_SCO_RECONNECT_DELAY_MS = 30_000L
        private const val MAX_SCO_RECONNECT_STEPS = 12
    }
}
