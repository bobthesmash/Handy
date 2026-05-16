package cz.handy.feature.actions.notification

import android.app.Notification
import android.app.NotificationManager

/**
 * Pravidla pro to, které notifikace uložit pro [READ_LAST_NOTIFICATION] ([F2-T01]).
 * Bez síťových volání; čistá logika vhodná pro unit testy.
 */
object NotificationSnapshotPolicy {
    /**
     * `true` = notifikaci neukládat (šum, progress, služby na pozadí, …).
     */
    fun shouldIgnoreForSnapshot(
        importance: Int,
        category: String?,
        ongoing: Boolean,
    ): Boolean {
        if (importance <= NotificationManager.IMPORTANCE_MIN) return true
        when (category) {
            Notification.CATEGORY_PROGRESS,
            Notification.CATEGORY_SERVICE,
            -> return true
            else -> Unit
        }
        if (ongoing && importance <= NotificationManager.IMPORTANCE_LOW) return true
        return false
    }

    /** `true` = nový snímek nahradí dříve uložený řádek. */
    fun shouldReplaceStored(
        newImportance: Int,
        newPostTime: Long,
        prevImportance: Int,
        prevPostTime: Long,
    ): Boolean {
        if (prevImportance < 0) return true
        if (newImportance > prevImportance) return true
        if (newImportance < prevImportance) return false
        return newPostTime >= prevPostTime
    }
}
