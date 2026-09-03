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
                    "call {contact}",
                    "ring {contact}",
                    "get {contact} on the phone",
                    "dial {contact}",
                    "phone {contact}",
                    "give {contact} a call",
                )
            }
            intent("SEND_SMS", requiresConfirm = true) {
                patterns(
                    "pošli sms {contact} že {message}",
                    "sms pro {contact} text {message}",
                    "text {contact} {message}",
                    "message {contact} {message}",
                    "tell {contact} {message}",
                    "send {contact} a text {message}",
                    "send sms to {contact} that {message}",
                    "send a text to {contact} that {message}",
                )
            }
            intent("SET_ALARM", requiresConfirm = true) {
                patterns(
                    "nastav budík na {time}",
                    "budík na {time}",
                    "set alarm for {time}",
                    "set an alarm for {time}",
                    "wake me up at {time}",
                    "alarm for {time}",
                    "set alarm at {time}",
                    "alarm at {time}",
                )
                phrase("budík čas {time} název {label}")
                phrase("set alarm for {time} called {label}")
                slot("label") { required = false }
            }
            intent("CANCEL", requiresConfirm = false) {
                patterns(
                    "zruš to",
                    "zruš",
                    "nechci to",
                    "cancel",
                    "never mind",
                    "forget it",
                    "drop it",
                    "abort",
                    "scratch that",
                    "no",
                    "nope",
                )
            }
            intent("STOP", requiresConfirm = false) {
                patterns(
                    "stop",
                    "zastav",
                    "přestaň mluvit",
                    "ticho",
                    "knock it off",
                    "be quiet",
                    "stop talking",
                    "shut up",
                    "hush",
                    "hold on",
                )
            }
            intent("REPEAT", requiresConfirm = false) {
                patterns(
                    "opakuj",
                    "zopakuj",
                    "řekni to znovu",
                    "co jsi řekl",
                    "say that again",
                    "repeat",
                    "one more time",
                    "what did you say",
                    "come again",
                    "say again",
                    "what was that again",
                )
            }
            intent("VOLUME", requiresConfirm = false) {
                phrase("{operation} hlasitost")
                phrase("úplně ztiš")
                phrase("volume {operation}")
                phrase("turn {operation} the volume")
                phrase("turn the volume {operation}")
                patterns(
                    "volume up{operation=up}",
                    "turn it up{operation=up}",
                    "louder{operation=up}",
                    "crank it{operation=up}",
                    "pump it up{operation=up}",
                    "turn up the volume{operation=up}",
                    "volume down{operation=down}",
                    "turn it down{operation=down}",
                    "quieter{operation=down}",
                    "turn down the volume{operation=down}",
                    "lower the volume{operation=down}",
                    "shut it{operation=mute}",
                    "shut up{operation=mute}",
                    "mute media{operation=mute}",
                )
                slot("operation") { required = false }
            }
            intent("READ_LAST_NOTIFICATION", requiresConfirm = false) {
                patterns(
                    "přečti poslední notifikaci",
                    "jaká je poslední notifikace",
                    "read last notification",
                    "what was that",
                    "read that",
                    "what just came in",
                    "check that message",
                    "what did I get",
                )
            }
            intent("REPLY_NOTIF", requiresConfirm = true) {
                patterns(
                    "odpověz na notifikaci že {message}",
                    "odpověz že {message}",
                    "napiš odpověď {message}",
                    "reply {message}",
                    "tell them {message}",
                    "say {message}",
                    "answer {message}",
                    "reply that {message}",
                    "answer that {message}",
                    "text back {message}",
                )
            }
            intent("PLAY_MEDIA", requiresConfirm = false) {
                patterns(
                    "přehraj hudbu",
                    "pusť hudbu",
                    "přehraj muziku",
                    "spusť přehrávání",
                    "play",
                    "play music",
                    "play media",
                    "start the music",
                    "resume",
                    "play some tunes",
                )
                phrase("přehraj {app}")
                phrase("pusť {app}")
                phrase("play {app}")
                slot("app") { required = false }
            }
            intent("TORCH", requiresConfirm = false) {
                phrase("{mode} baterku")
                phrase("{mode} flashlight")
                phrase("{mode} torch")
                phrase("turn {mode} flashlight")
                phrase("turn {mode} the flashlight")
                phrase("turn {mode} torch")
                phrase("{mode} the flashlight")
                phrase("flashlight {mode}")
                phrase("torch {mode}")
                patterns(
                    "turn on flashlight{mode=on}",
                    "flashlight on{mode=on}",
                    "gimme a light{mode=on}",
                    "light on{mode=on}",
                    "torch on{mode=on}",
                    "turn the light on{mode=on}",
                    "turn off flashlight{mode=off}",
                    "flashlight off{mode=off}",
                    "kill the light{mode=off}",
                    "light off{mode=off}",
                    "torch off{mode=off}",
                    "turn the light off{mode=off}",
                )
            }
            intent("WHAT_TIME", requiresConfirm = false) {
                patterns(
                    "kolik je hodin",
                    "kolik máme hodin",
                    "jaký je čas",
                    "what time is it",
                    "what's the time",
                    "got the time",
                    "time check",
                    "tell me the time",
                    "what time we got",
                )
            }
            intent("WHAT_DATE", requiresConfirm = false) {
                patterns(
                    "jaký je den",
                    "jaké je datum",
                    "kolikátého je",
                    "what's the date",
                    "what day is it",
                    "what's today",
                    "date check",
                    "what's today's date",
                    "what is the date",
                    "what date is it",
                )
            }
            intent("WHAT_BATTERY", requiresConfirm = false) {
                patterns(
                    "kolik mám baterky",
                    "jaká je baterka",
                    "stav baterie",
                    "how's my battery",
                    "battery level",
                    "how much juice",
                    "battery dying",
                    "what's my battery",
                    "how much battery",
                    "battery percentage",
                )
            }
            intent("OPEN_APP", requiresConfirm = false) {
                patterns(
                    "otevři {app}",
                    "spusť {app}",
                    "zapni aplikaci {app}",
                    "open {app}",
                    "launch {app}",
                    "start {app}",
                    "switch to {app}",
                    "bring up {app}",
                    "pull up {app}",
                )
            }
            intent("NAVIGATE", requiresConfirm = false) {
                patterns(
                    "naviguj na {place}",
                    "ukaž na mapách {place}",
                    "trasa do {place}",
                    "navigate to {place}",
                    "take me to {place}",
                    "directions to {place}",
                    "get me to {place}",
                    "drive to {place}",
                    "route to {place}",
                    "how do I get to {place}",
                )
            }
            intent("TIMER", requiresConfirm = false) {
                patterns(
                    "časovač {duration}",
                    "timer {duration}",
                    "odpočet {duration}",
                    "set a timer for {duration}",
                    "start a timer for {duration}",
                    "count down {duration}",
                    "timer for {duration}",
                    "give me a timer for {duration}",
                )
            }
            intent("SET_CONTACT_ALIAS", requiresConfirm = false) {
                patterns(
                    "nazývej {target} jako {alias}",
                    "říkej {target} jako {alias}",
                    "ulož alias {alias} pro kontakt {target}",
                    "kontakt {target} říkej {alias}",
                    "call {target} {alias}",
                    "call {target} as {alias}",
                    "alias {target} as {alias}",
                    "nickname {target} {alias}",
                    "set alias {alias} for {target}",
                    "remember {target} as {alias}",
                )
            }
            intent("REMOVE_CONTACT_ALIAS", requiresConfirm = false) {
                patterns(
                    "smaž alias {alias}",
                    "zapomeň alias {alias}",
                    "odstraň alias {alias}",
                    "delete alias {alias}",
                    "remove alias {alias}",
                    "forget alias {alias}",
                    "clear alias {alias}",
                    "drop alias {alias}",
                    "erase alias {alias}",
                )
            }
            intent("MEDIA_CTRL", requiresConfirm = false) {
                phrase("{command} hudba")
                phrase("{command} skladba")
                patterns(
                    "pause{command=pause}",
                    "next{command=next}",
                    "skip{command=skip}",
                    "previous{command=previous}",
                    "go back{command=previous}",
                    "next track{command=next}",
                    "skip song{command=skip}",
                    "skip track{command=skip}",
                )
            }
            intent("UNLOCK_SCREEN", requiresConfirm = false) {
                patterns(
                    "odemkni obrazovku",
                    "odemkni",
                    "unlock screen",
                    "unlock the phone",
                    "wake the screen",
                    "unlock",
                    "wake up screen",
                    "turn on screen",
                )
            }
            intent("CONFIRM", requiresConfirm = false) {
                patterns(
                    "ano",
                    "jo",
                    "potvrzuji",
                    "yes",
                    "yeah",
                    "yep",
                    "ok",
                    "okay",
                    "sure",
                    "sounds good",
                    "do it",
                )
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
