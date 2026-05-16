package cz.handy.feature.actions.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Zrcadlí významné notifikace lokálně pro [READ_LAST_NOTIFICATION] ([F1-T13], [F2-T01]).
 * [REPLY_NOTIF] ([F2-T02]) posílá text přes první [RemoteInput] na uložené [StatusBarNotification].
 */
class HandyNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        synchronized(Lock) { runningInstance = this }
        NotificationSnapshotStore.setListenerIoConnected(this, true)
        val snapshots = activeNotifications ?: return
        var bestLine: String? = null
        var bestImportance = Int.MIN_VALUE
        var bestPostTime = Long.MIN_VALUE
        var bestSbn: StatusBarNotification? = null
        for (sbn in snapshots) {
            val line = extractReadableLine(sbn) ?: continue
            val importance = resolveImportance(sbn)
            val ongoing = sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0
            if (
                NotificationSnapshotPolicy.shouldIgnoreForSnapshot(
                    importance,
                    sbn.notification.category,
                    ongoing,
                )
            ) {
                continue
            }
            if (
                !NotificationSnapshotPolicy.shouldReplaceStored(
                    importance,
                    sbn.postTime,
                    bestImportance,
                    bestPostTime,
                )
            ) {
                continue
            }
            bestImportance = importance
            bestPostTime = sbn.postTime
            bestLine = line
            bestSbn = sbn
        }
        if (bestLine != null && bestSbn != null && bestImportance != Int.MIN_VALUE) {
            persistSnapshot(bestSbn, bestLine, bestImportance, bestPostTime)
        }
    }

    override fun onListenerDisconnected() {
        NotificationSnapshotStore.setListenerIoConnected(this, false)
        synchronized(Lock) {
            if (runningInstance === this) {
                runningInstance = null
            }
        }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val line = extractReadableLine(sbn) ?: return
        val importance = resolveImportance(sbn)
        val ongoing = sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0
        if (
            NotificationSnapshotPolicy.shouldIgnoreForSnapshot(
                importance,
                sbn.notification.category,
                ongoing,
            )
        ) {
            return
        }
        persistSnapshot(sbn, line, importance, sbn.postTime)
    }

    private fun persistSnapshot(
        sbn: StatusBarNotification,
        line: String,
        importance: Int,
        postTime: Long,
    ) {
        NotificationSnapshotStore.updateFromNotification(
            this,
            line,
            importance,
            postTime,
            sbnKey = sbn.key,
            packageName = sbn.packageName,
            canReply = hasInlineReply(sbn.notification),
        )
    }

    private fun resolveImportance(sbn: StatusBarNotification): Int {
        val ranking = Ranking()
        return if (currentRanking.getRanking(sbn.key, ranking)) {
            ranking.importance
        } else {
            legacyImportance(sbn.notification)
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyImportance(n: Notification): Int =
        when (n.priority) {
            Notification.PRIORITY_MAX -> NotificationManager.IMPORTANCE_MAX
            Notification.PRIORITY_HIGH -> NotificationManager.IMPORTANCE_HIGH
            Notification.PRIORITY_DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
            Notification.PRIORITY_LOW -> NotificationManager.IMPORTANCE_LOW
            Notification.PRIORITY_MIN -> NotificationManager.IMPORTANCE_MIN
            else -> NotificationManager.IMPORTANCE_DEFAULT
        }

    private fun extractReadableLine(sbn: StatusBarNotification): String? {
        val extras = sbn.notification.extras
        val title =
            (
                extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    ?: ""
            ).trim()
        val text =
            (
                extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                    ?: ""
            ).trim()
        val big =
            (
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                    ?: ""
            ).trim()
        val body = big.ifBlank { text }
        val extraLines =
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        val joined =
            extraLines
                ?.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
                ?.takeIf { it.isNotEmpty() }
                ?.joinToString("\n")
                ?: ""
        if (joined.isNotBlank() && body.isBlank()) {
            return formatTitleBody(title, joined)
        }
        return formatTitleBody(title, body)
    }

    private fun formatTitleBody(
        title: String,
        body: String,
    ): String? {
        val line =
            when {
                title.isNotBlank() && body.isNotBlank() -> "$title — $body"
                title.isNotBlank() -> title
                body.isNotBlank() -> body
                else -> return null
            }
        return line
    }

    private fun hasInlineReply(n: Notification): Boolean {
        val actions = n.actions ?: return false
        for (action in actions) {
            val inputs = action.remoteInputs
            if (inputs != null && inputs.isNotEmpty()) return true
        }
        return false
    }

    private fun sendRemoteInputReply(
        sbn: StatusBarNotification,
        reply: String,
    ): Boolean {
        val actions = sbn.notification.actions ?: return false
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            if (remoteInputs.isEmpty()) continue
            val fillInIntent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val results = Bundle()
            for (ri in remoteInputs) {
                results.putCharSequence(ri.resultKey, reply)
            }
            RemoteInput.addResultsToIntent(remoteInputs, fillInIntent, results)
            try {
                action.actionIntent.send(this, 0, fillInIntent)
                return true
            } catch (_: PendingIntent.CanceledException) {
                continue
            }
        }
        return false
    }

    companion object {
        private val Lock = Any()

        @Volatile
        private var runningInstance: HandyNotificationListenerService? = null

        /**
         * Odešle uložené inline reply na poslední snímek (WhatsApp / Signal / SMS…), pokud je
         * [NotificationSnapshotStore.lastCanReply] a služba je připojená.
         */
        fun trySendReplyFromSnapshot(
            app: Context,
            message: String,
        ): Result<String> {
            val text = message.trim()
            if (text.isBlank()) {
                return Result.failure(IllegalArgumentException("Prázdná odpověď."))
            }
            val ctx = app.applicationContext
            val key = NotificationSnapshotStore.readLastSbnKey(ctx)
            val svc = synchronized(Lock) { runningInstance }
            val sbn = svc?.activeNotifications?.firstOrNull { it.key == key }
            val failure: IllegalStateException? =
                when {
                    !NotificationSnapshotStore.listenerEnabled(ctx) ->
                        IllegalStateException(
                            "Ve „Nastavení → Oznámení → přístup k oznámením“ povolte Handy jako posluchače.",
                        )
                    !NotificationSnapshotStore.lastCanReply(ctx) ->
                        IllegalStateException(
                            "Poslední uložená notifikace nepodporuje odpověď z této obrazovky.",
                        )
                    key == null ->
                        IllegalStateException("Chybí reference na notifikaci.")
                    svc == null ->
                        IllegalStateException(
                            "Posluchač oznámení zrovna není aktivní. Počkejte na příchozí notifikaci nebo otevřete Handy.",
                        )
                    sbn == null ->
                        IllegalStateException(
                            "Notifikace už není na panelu — odpověď nelze odeslat.",
                        )
                    else -> null
                }
            if (failure != null) {
                return Result.failure(failure)
            }
            val activeSvc = requireNotNull(svc)
            val activeSbn = requireNotNull(sbn)
            return if (activeSvc.sendRemoteInputReply(activeSbn, text)) {
                Result.success("Odpověď odeslána.")
            } else {
                Result.failure(
                    IllegalStateException(
                        "Odeslání odpovědi selhalo (aplikace změnila akce nebo odpověď není dostupná).",
                    ),
                )
            }
        }
    }
}
