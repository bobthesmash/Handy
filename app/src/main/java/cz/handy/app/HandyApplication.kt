package cz.handy.app

import android.app.Application
import android.util.Log
import cz.handy.app.crash.DeviceCrashLogger
import cz.handy.feature.wakeword.PorcupineEarWakePump
import cz.handy.feature.wakeword.WakeWordEnginesProbe
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class HandyApplication : Application() {
    private val appScope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Default +
                CoroutineExceptionHandler { _, e ->
                    Log.w(APP_TAG, "Background coroutine failed", e)
                },
        )

    override fun onCreate() {
        super.onCreate()
        DeviceCrashLogger.install(this)
        PorcupineEarWakePump.startIfAccessKeyConfigured(this, appScope)
        if (BuildConfig.DEBUG) {
            Log.d(APP_TAG, "Scheduling F0-T05 WakeWordEnginesProbe")
            appScope.launch {
                runCatching {
                    WakeWordEnginesProbe.run(this@HandyApplication)
                }.onFailure {
                    Log.w(APP_TAG, "WakeWordEnginesProbe failed", it)
                }
            }
        }
    }

    private companion object {
        const val APP_TAG = "HandyApp"
    }
}
