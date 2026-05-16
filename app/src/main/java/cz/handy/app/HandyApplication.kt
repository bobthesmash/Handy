package cz.handy.app

import android.app.Application
import android.util.Log
import cz.handy.feature.wakeword.PorcupineEarWakePump
import cz.handy.feature.wakeword.WakeWordEnginesProbe
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class HandyApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
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
