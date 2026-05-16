package cz.handy.feature.actions.executor

import android.annotation.SuppressLint
import android.content.Context
import cz.handy.core.persistence.ContactAliasStore
import cz.handy.feature.actions.alarm.AlarmClockIntentStarter
import cz.handy.feature.actions.alarm.AlarmSlotTimeParser
import cz.handy.feature.actions.app.OpenAppLauncher
import cz.handy.feature.actions.audio.MediaVolumeAdjuster
import cz.handy.feature.actions.info.DeviceInfoAnswers
import cz.handy.feature.actions.media.MediaCtrlCommandParser
import cz.handy.feature.actions.media.MediaPlaybackHandover
import cz.handy.feature.actions.nav.MapsNavigateLauncher
import cz.handy.feature.actions.notification.HandyNotificationListenerService
import cz.handy.feature.actions.notification.NotificationSnapshotStore
import cz.handy.feature.actions.phone.TelecomCallPlacer
import cz.handy.feature.actions.sms.SmsTextMessageSender
import cz.handy.feature.actions.timer.CzechDurationParser
import cz.handy.feature.actions.timer.TimerClockIntentStarter
import cz.handy.feature.actions.torch.TorchModeSwitcher
import cz.handy.feature.nlu.ParsedIntent

/**
 * Jednotné provedení MVP intentů a info intentů [F2-T07] nad systémovými API ([F1-T09]+).
 *
 * `@param smsExplicitConfirm` jen po hlasovém confirm gate (`SEND_SMS`), jinde `false`.
 */
@SuppressLint("MissingPermission")
class MvpIntentExecutor(
    context: Context,
) {
    private val app = context.applicationContext
    private val callPlacer = TelecomCallPlacer(app)
    private val smsSender = SmsTextMessageSender(app)
    private val volumeAdjuster = MediaVolumeAdjuster(app)
    private val torch = TorchModeSwitcher(app)
    private val mediaHandover = MediaPlaybackHandover(app)
    private val openApp = OpenAppLauncher(app)
    private val mapsNav = MapsNavigateLauncher(app)
    private val contactAliases = ContactAliasStore(app)

    @Suppress("ReturnCount")
    fun execute(
        parsed: ParsedIntent,
        smsExplicitConfirm: Boolean,
    ): Result<String> =
        when (parsed.intentId) {
            "CALL" -> execCall(parsed)
            "SEND_SMS" -> execSms(parsed, smsExplicitConfirm)
            "SET_ALARM" -> execAlarm(parsed)
            "VOLUME" -> execVolume(parsed)
            "READ_LAST_NOTIFICATION" -> execReadNotification()
            "REPLY_NOTIF" -> execReplyNotif(parsed)
            "PLAY_MEDIA" -> execPlayMedia(parsed)
            "MEDIA_CTRL" -> execMediaCtrl(parsed)
            "TORCH" -> execTorch(parsed)
            "WHAT_TIME" -> Result.success(DeviceInfoAnswers.timeSentence())
            "WHAT_DATE" -> Result.success(DeviceInfoAnswers.dateSentence())
            "WHAT_BATTERY" -> DeviceInfoAnswers.batterySentence(app)
            "OPEN_APP" -> execOpenApp(parsed)
            "NAVIGATE" -> execNavigate(parsed)
            "TIMER" -> execTimer(parsed)
            "SET_CONTACT_ALIAS" -> execSetContactAlias(parsed)
            "REMOVE_CONTACT_ALIAS" -> execRemoveContactAlias(parsed)
            else ->
                Result.failure(
                    UnsupportedOperationException("Neznámý intent: ${parsed.intentId}"),
                )
        }

    private fun execCall(parsed: ParsedIntent): Result<String> {
        val contact = parsed.slots["contact"].orEmpty()
        callPlacer
            .placeOutgoingCall(contact)
            .onFailure { return Result.failure(it) }
        return Result.success("Spouštím hovor…")
    }

    private fun execSms(
        parsed: ParsedIntent,
        smsExplicitConfirm: Boolean,
    ): Result<String> {
        val contact = parsed.slots["contact"].orEmpty()
        val message = parsed.slots["message"].orEmpty()
        smsSender
            .sendTextMessage(smsExplicitConfirm, contact, message)
            .onFailure { return Result.failure(it) }
        return Result.success("SMS odeslána.")
    }

    private fun execAlarm(parsed: ParsedIntent): Result<String> {
        val timeRaw = parsed.slots["time"].orEmpty()
        val hm =
            AlarmSlotTimeParser.parseHourMinute(timeRaw)
                ?: return Result.failure(
                    IllegalArgumentException(
                        "Nerozuměl jsem času budíku: $timeRaw",
                    ),
                )

        val label = parsed.slots["label"]?.takeIf { it.isNotBlank() }
        val ok =
            AlarmClockIntentStarter.tryStartAlarm(app, hm.first, hm.second, label)
        return if (ok) {
            Result.success("Otevírám systémové hodiny na nastavení budíku (${hm.first}:${hm.second.toString().padStart(2, '0')}).")
        } else {
            Result.failure(IllegalStateException("Nelze spustit obrazovku budíku (žádná aplikace ho neobsluhuje?)."))
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun execVolume(parsed: ParsedIntent): Result<String> {
        val rawOp = parsed.slots["operation"]?.trim().orEmpty()

        /** Druhá šablona intentu bez slotu ⇒ znamená mute. */
        if (parsed.slots.isEmpty() || rawOp.isEmpty()) {
            return Result.success(volumeAdjuster.muteAllMedia())
        }

        fun isUp(op: String) = op.startsWith("zvyš") || op.startsWith("zvýš") || op.startsWith("zvěš")

        fun isDown(op: String) = op.startsWith("sniž") || op.startsWith("sníž")

        return Result.success(
            when {
                isUp(rawOp) -> volumeAdjuster.volumeUp()
                isDown(rawOp) -> volumeAdjuster.volumeDown()
                rawOp.startsWith("ztiš") ||
                    rawOp.contains("mute") -> volumeAdjuster.muteAllMedia()
                else ->
                    return Result.failure(
                        IllegalArgumentException("Neplatná změna hlasitosti: $rawOp"),
                    )
            },
        )
    }

    private fun execReadNotification(): Result<String> {
        if (!NotificationSnapshotStore.listenerEnabled(app)) {
            return Result.failure(
                IllegalStateException(
                    "Ve „Nastavení → Oznámení → přístup k oznámením“ povolte Handy jako posluchače.",
                ),
            )
        }

        val line =
            NotificationSnapshotStore.readLatest(app)
                ?: return Result.failure(
                    IllegalStateException("Zatím žádná uložená notifikace."),
                )
        /** Stejný text čte asistent hlasem po exekuci ([F1-T15]). */
        return Result.success(line)
    }

    private fun execReplyNotif(parsed: ParsedIntent): Result<String> {
        val message = parsed.slots["message"].orEmpty()
        return HandyNotificationListenerService.trySendReplyFromSnapshot(app, message)
    }

    private fun execPlayMedia(parsed: ParsedIntent): Result<String> {
        val appSlot = parsed.slots["app"]?.takeIf { it.isNotBlank() }
        return mediaHandover.play(appSlot)
    }

    private fun execMediaCtrl(parsed: ParsedIntent): Result<String> {
        val cmd = parsed.slots["command"].orEmpty()
        val op =
            MediaCtrlCommandParser.parse(cmd)
                ?: return Result.failure(
                    IllegalArgumentException("Nerozuměl jsem mediálnímu příkazu."),
                )
        return mediaHandover.transport(op)
    }

    private fun execOpenApp(parsed: ParsedIntent): Result<String> {
        val appSlot = parsed.slots["app"].orEmpty()
        return openApp.openBySpeechAlias(appSlot)
    }

    private fun execNavigate(parsed: ParsedIntent): Result<String> {
        val place = parsed.slots["place"].orEmpty()
        return mapsNav.openPlaceQuery(place)
    }

    private fun execTimer(parsed: ParsedIntent): Result<String> {
        val raw = parsed.slots["duration"].orEmpty()
        val sec =
            CzechDurationParser.parseToSeconds(raw)
                ?: return Result.failure(
                    IllegalArgumentException("Nerozuměl jsem délce časovače: $raw"),
                )
        val ok = TimerClockIntentStarter.tryStartTimer(app, sec)
        return if (ok) {
            Result.success("Otevírám systémový časovač ($sec s).")
        } else {
            Result.failure(
                IllegalStateException("Nelze spustit obrazovku časovače (žádná aplikace ho neobsluhuje?)."),
            )
        }
    }

    private fun execSetContactAlias(parsed: ParsedIntent): Result<String> {
        val alias = parsed.slots["alias"]?.trim().orEmpty()
        val target = parsed.slots["target"]?.trim().orEmpty()
        if (alias.isEmpty() || target.isEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    "Řekni například „nazývej Petr Vondrák jako bratr“ — potřebuji jméno nebo číslo i přezdívku.",
                ),
            )
        }
        return contactAliases.upsert(aliasSpoken = alias, contactQuery = target).fold(
            onSuccess = {
                Result.success(
                    "Uloženo: až řekneš „$alias“, použiju kontakt „$target“.",
                )
            },
            onFailure = { Result.failure(it) },
        )
    }

    private fun execRemoveContactAlias(parsed: ParsedIntent): Result<String> {
        val alias = parsed.slots["alias"]?.trim().orEmpty()
        if (alias.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Řekni například „smaž alias bratr“."),
            )
        }
        contactAliases.remove(alias)
        return Result.success("Alias „$alias“ je smazaný.")
    }

    private fun execTorch(parsed: ParsedIntent): Result<String> {
        val mode =
            parsed.slots["mode"]
                ?.trim()
                ?.lowercase()
                .orEmpty()

        fun wantsOff(m: String) =
            m.startsWith("zhas") ||
                m.startsWith("vypni") ||
                m.startsWith("vypn") ||
                m.startsWith("vypí")

        fun wantsOn(m: String) =
            m.startsWith("zapni") ||
                m.startsWith("zapně") ||
                m.startsWith("zapín") ||
                m.startsWith("zvyš") ||
                m.startsWith("zvýš") ||
                m.startsWith("rozsv")

        val on = wantsOn(mode)
        val off = wantsOff(mode)

        val wantOn =
            when {
                on && off ->
                    return Result.failure(
                        IllegalArgumentException("Rozpor v pokynu ke svítilně."),
                    )

                off -> false
                on -> true
                else ->
                    return Result.failure(
                        IllegalArgumentException("Neplatný pokyn ke svítilně: \"$mode\""),
                    )
            }

        torch
            .setTorch(wantOn)
            .onFailure { return Result.failure(it) }
        return Result.success(
            if (wantOn) "Svítilna zapnutá." else "Svítilna vypnutá.",
        )
    }
}
