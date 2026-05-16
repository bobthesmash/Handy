package cz.handy.feature.actions.notification

import android.content.Context
import android.provider.Settings

private const val PREF = "handy_notification_snapshot"
private const val KEY_TEXT = "last_notification_line"
private const val KEY_TIME = "last_updated_ms"
private const val KEY_IMPORTANCE = "last_snapshot_importance"
private const val KEY_POST_TIME = "last_snapshot_post_time"
private const val KEY_LISTENER_IO = "listener_io_connected"
private const val KEY_SBN_KEY = "last_sbn_key"
private const val KEY_PACKAGE = "last_notification_package"
private const val KEY_CAN_REPLY = "last_notification_can_reply"

/** Poslední text notifikace pro intent `READ_LAST_NOTIFICATION` bez odesílání audia ven. */
object NotificationSnapshotStore {
    private const val LISTENER_CLASS = "cz.handy.feature.actions.notification.HandyNotificationListenerService"

    /**
     * Uloží řádek jen pokud projde prioritou vůči předchozímu snímku ([F2-T01]).
     *
     * @param postTime [android.service.notification.StatusBarNotification.getPostTime]
     * @param importance [NotificationListenerService.Ranking.importance] nebo legacy mapa z priority.
     */
    fun updateFromNotification(
        context: Context,
        line: String,
        importance: Int,
        postTime: Long,
        sbnKey: String,
        packageName: String,
        canReply: Boolean,
    ) {
        val trimmed = line.trim().ifBlank { return }
        val sp = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val prevImp = sp.getInt(KEY_IMPORTANCE, -1)
        val prevPost = sp.getLong(KEY_POST_TIME, 0L)
        if (!NotificationSnapshotPolicy.shouldReplaceStored(importance, postTime, prevImp, prevPost)) {
            return
        }
        sp
            .edit()
            .putString(KEY_TEXT, trimmed)
            .putLong(KEY_TIME, System.currentTimeMillis())
            .putInt(KEY_IMPORTANCE, importance)
            .putLong(KEY_POST_TIME, postTime)
            .putString(KEY_SBN_KEY, sbnKey)
            .putString(KEY_PACKAGE, packageName)
            .putBoolean(KEY_CAN_REPLY, canReply)
            .apply()
    }

    fun readLastSbnKey(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_SBN_KEY, null)

    fun lastCanReply(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_CAN_REPLY, false)

    fun readLatest(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_TEXT, null)

    fun listenerEnabled(context: Context): Boolean =
        runCatching {
            val flat =
                Settings.Secure
                    .getString(
                        context.contentResolver,
                        "enabled_notification_listeners",
                    ).orEmpty()
            flat.contains(LISTENER_CLASS)
        }.getOrDefault(false)

    /** Služba zrovna běží a dostává callbacky (I/O smyčka NLS). */
    fun setListenerIoConnected(
        context: Context,
        connected: Boolean,
    ) {
        context.applicationContext
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LISTENER_IO, connected)
            .apply()
    }

    fun listenerIoConnected(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_LISTENER_IO, false)
}
