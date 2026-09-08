package cz.handy.feature.asr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import cz.handy.core.common.asr.CoarseCountrySignals
import java.util.Locale

/**
 * Reads last-known location / network / SIM / locale for [cz.handy.core.common.asr.PlaceAsrLanguageSelector].
 * Never throws on missing permission — logs and leaves that signal empty.
 */
class CoarseCountrySignalsReader(
    context: Context,
) {
    private val app = context.applicationContext

    fun read(): CoarseCountrySignals {
        val locPerm = hasLocationPermission()
        var lat: Double? = null
        var lon: Double? = null
        var locReason: String? = null
        if (!locPerm) {
            locReason = "ACCESS_COARSE_LOCATION not granted"
            Log.i(TAG, "skip last-known location: $locReason — using network/SIM/locale")
        } else {
            val loc = lastKnownOrNull()
            if (loc == null) {
                locReason = "no last-known location"
                Log.i(TAG, "skip last-known location: $locReason — using network/SIM/locale")
            } else {
                lat = loc.latitude
                lon = loc.longitude
            }
        }
        val tm = runCatching { app.getSystemService(TelephonyManager::class.java) }.getOrNull()
        val network = runCatching { tm?.networkCountryIso }.getOrNull()
        val sim = runCatching { tm?.simCountryIso }.getOrNull()
        return CoarseCountrySignals(
            lastKnownLat = lat,
            lastKnownLon = lon,
            locationPermissionGranted = locPerm,
            lastKnownUnavailableReason = locReason,
            networkCountryIso = network,
            simCountryIso = sim,
            localeCountryIso = Locale.getDefault().country,
        )
    }

    private fun hasLocationPermission(): Boolean {
        val coarse =
            ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val fine =
            ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownOrNull(): Location? {
        val lm = app.getSystemService(LocationManager::class.java) ?: return null
        val providers =
            listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
        var best: Location? = null
        for (provider in providers) {
            val loc =
                try {
                    lm.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    Log.w(TAG, "last-known location denied for $provider", e)
                    return null
                } catch (e: RuntimeException) {
                    Log.w(TAG, "last-known location failed for $provider", e)
                    continue
                }
            if (loc == null) continue
            if (best == null || loc.time > best.time) best = loc
        }
        return best
    }

    private companion object {
        const val TAG = "HandyPlaceAsr"
    }
}
