package cz.handy.feature.nlu

/** Výchozí pravidlové intenty MVP (lze nahradit vlastním [IntentCatalog]). */
object HandyNluCatalogs {
    val mvp: IntentCatalog =
        intentCatalog {
            intent("CALL", requiresConfirm = true) {
                patterns(
                    "zavolej číslo {contact}",
                    "zavolej {contact}",
                    "vytoč {contact}",
                )
            }
            intent("SEND_SMS", requiresConfirm = true) {
                patterns(
                    "pošli sms {contact} že {message}",
                    "sms pro {contact} text {message}",
                )
            }
            intent("SET_ALARM", requiresConfirm = true) {
                patterns(
                    "nastav budík na {time}",
                    "budík na {time}",
                )
                phrase("budík čas {time} název {label}")
                slot("label") { required = false }
            }
            intent("CANCEL", requiresConfirm = false) {
                patterns(
                    "zruš to",
                    "zruš",
                    "nechci to",
                    "cancel",
                )
            }
            intent("STOP", requiresConfirm = false) {
                patterns(
                    "stop",
                    "zastav",
                    "přestaň mluvit",
                    "ticho",
                )
            }
            intent("REPEAT", requiresConfirm = false) {
                patterns(
                    "opakuj",
                    "zopakuj",
                    "řekni to znovu",
                    "co jsi řekl",
                )
            }
            intent("VOLUME", requiresConfirm = false) {
                phrase("{operation} hlasitost")
                phrase("úplně ztiš")
                slot("operation") { required = false }
            }
            intent("READ_LAST_NOTIFICATION", requiresConfirm = false) {
                patterns(
                    "přečti poslední notifikaci",
                    "jaká je poslední notifikace",
                )
            }
            intent("REPLY_NOTIF", requiresConfirm = true) {
                patterns(
                    "odpověz na notifikaci že {message}",
                    "odpověz že {message}",
                    "napiš odpověď {message}",
                )
            }
            intent("PLAY_MEDIA", requiresConfirm = false) {
                patterns(
                    "přehraj hudbu",
                    "pusť hudbu",
                    "přehraj muziku",
                    "spusť přehrávání",
                )
                phrase("přehraj {app}")
                phrase("pusť {app}")
                slot("app") { required = false }
            }
            intent("TORCH", requiresConfirm = false) {
                phrase("{mode} baterku")
            }
            intent("WHAT_TIME", requiresConfirm = false) {
                patterns(
                    "kolik je hodin",
                    "kolik máme hodin",
                    "jaký je čas",
                )
            }
            intent("WHAT_DATE", requiresConfirm = false) {
                patterns(
                    "jaký je den",
                    "jaké je datum",
                    "kolikátého je",
                )
            }
            intent("WHAT_BATTERY", requiresConfirm = false) {
                patterns(
                    "kolik mám baterky",
                    "jaká je baterka",
                    "stav baterie",
                )
            }
            intent("OPEN_APP", requiresConfirm = false) {
                patterns(
                    "otevři {app}",
                    "spusť {app}",
                    "zapni aplikaci {app}",
                )
            }
            intent("NAVIGATE", requiresConfirm = false) {
                patterns(
                    "naviguj na {place}",
                    "ukaž na mapách {place}",
                    "trasa do {place}",
                )
            }
            intent("TIMER", requiresConfirm = false) {
                patterns(
                    "časovač {duration}",
                    "timer {duration}",
                    "odpočet {duration}",
                )
            }
            intent("SET_CONTACT_ALIAS", requiresConfirm = false) {
                patterns(
                    "nazývej {target} jako {alias}",
                    "říkej {target} jako {alias}",
                    "ulož alias {alias} pro kontakt {target}",
                    "kontakt {target} říkej {alias}",
                )
            }
            intent("REMOVE_CONTACT_ALIAS", requiresConfirm = false) {
                patterns(
                    "smaž alias {alias}",
                    "zapomeň alias {alias}",
                    "odstraň alias {alias}",
                )
            }
            intent("MEDIA_CTRL", requiresConfirm = false) {
                phrase("{command} hudba")
                phrase("{command} skladba")
            }
        }

    /**
     * Anglické vzory pro základní příkazy — používá se jen při zapnutém overlay v nastavení ([F5-T03]).
     * ASR zůstává primárně CZ model; overlay pomůže při smíšených frázích nebo budoucím EN ASR.
     */
    val enMinimal: IntentCatalog =
        intentCatalog {
            intent("CALL", requiresConfirm = true) {
                patterns("call {contact}", "phone {contact}", "dial {contact}")
            }
            intent("TORCH", requiresConfirm = false) {
                phrase("{mode} flashlight")
            }
            intent("WHAT_TIME", requiresConfirm = false) {
                patterns(
                    "what time is it",
                    "what's the time",
                )
            }
            intent("WHAT_DATE", requiresConfirm = false) {
                patterns(
                    "what date is it",
                    "what's the date",
                )
            }
            intent("WHAT_BATTERY", requiresConfirm = false) {
                patterns(
                    "battery status",
                    "how much battery",
                )
            }
            intent("CANCEL", requiresConfirm = false) {
                patterns("cancel", "abort")
            }
            intent("STOP", requiresConfirm = false) {
                patterns("stop", "quiet", "silence")
            }
            intent("REPEAT", requiresConfirm = false) {
                patterns("repeat", "say that again")
            }
            intent("OPEN_APP", requiresConfirm = false) {
                patterns("open {app}", "launch {app}", "start {app}")
            }
            intent("NAVIGATE", requiresConfirm = false) {
                patterns(
                    "navigate to {place}",
                    "directions to {place}",
                )
            }
            intent("TIMER", requiresConfirm = false) {
                patterns("timer {duration}", "countdown {duration}")
            }
        }
}
