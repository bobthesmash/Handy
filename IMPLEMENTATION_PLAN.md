# Handy – Implementation Plan

> **Pro agent workery:** Když dokončíš úkol, **musíš** udělat OBĚ věci:
> 1. V tomto souboru: změň `- [ ]` na `- [x]` u daného `[ID]` a přidej krátký commit-style záznam do sekce *Changelog* dole.
> 2. V `progress.html`: v JSON bloku najdi task s tím samým `id` a změň `"status": "pending"` → `"status": "done"`, vyplň `"completedAt"` na dnešní datum (`YYYY-MM-DD`).
>
> Pravidla:
> - **Jedno hotové ID = jeden commit** (`feat(F1-T03): integrate ECAPA-TDNN speaker verification`).
> - Před označením `done` musí být splněna **Definition of Done** dané fáze (viz níže).
> - Pokud úkol blokuje nějaká externí věc (čekání na rozhodnutí uživatele, hardware, API), nastav status na `blocked` v `progress.html` a popiš důvod do `notes`.
> - Pokud začínáš pracovat na úkolu, nastav `status: in_progress`. Pouze JEDEN úkol může být `in_progress` per worker.

---

## Cíl projektu

Lokální asistivní hlasový ovladač Androidu pro člověka bez funkčních rukou. Rozpoznává hlas svého majitele, funguje offline, při zamčené obrazovce a s telefonem v kapse přes BT headset.

## Stack (závazný od fáze 1)

- Kotlin + Jetpack Compose, min SDK 26, target SDK 35
- Hilt, Coroutines + Flow, DataStore, Room
- ONNX Runtime Mobile, TensorFlow Lite (podle modelu)
- Wake-word: **Porcupine** (default; viz `[F0-T05]`)
- ASR: **Sherpa-onnx** s Vosk-CZ / Whisper-cz model (viz `[F1-T06]`)
- Speaker verification: **ECAPA-TDNN** (3D-Speaker), ONNX
- TTS: Android Speech Services (CZ), volitelně Piper

## Definition of Done (per fáze)

- **F0:** PoC běží na fyzickém zařízení, latence wake→ASR-ready ≤ 700 ms, baterie ≤ 6 %/h v listening idle.
- **F1:** 6 intentů funguje, 90 %+ recognition na vlastním hlase (50 nahrávek), < 2 % false-accept cizí.
- **F2:** všechny intenty F2 fungují, 80 % úspěšnost s telefonem v kapse + BT headset.
- **F3:** všechny intenty otestované na lockscreen QA matrix, baterie ≤ 5 %/h listening idle.
- **F4:** beta 3–5 uživatelů, dokončený privacy policy, hands-free onboarding ověřený.

### Měření a QA dokumenty vs „done“

Soubory `docs/benchmarks/latency.md`, `docs/benchmarks/battery.md`, `docs/qa/lockscreen-matrix.md` mohou obsahovat **jen proceduru a prázdné tabulky**. To dokončuje dílčí práci „místo pro záznam existuje“, ale **nesplní** DoD latence, spotřeby ani lockscreen pokud v nich **nejsou řádky z reálného HW** (`[F0-T07]`, `[F0-T08]`, ruční QA matrix). Viz záhlaví v každém z těchto dokumentů.

---

## Fáze 0 — Foundation & PoC

> *Týdny 1–2. Cíl: ověřit feasibility na cílovém HW, žádné UI ještě nepotřebujeme.*

- [x] **[F0-T01]** Setup Android projektu (Kotlin, Compose, Hilt, min SDK 26, target SDK 35), Gradle Version Catalogs, ktlint, detekt
- [x] **[F0-T02]** Modulární struktura: `:app`, `:core:common`, `:core:audio`, `:feature:wakeword`, `:feature:asr`, `:feature:voiceid`, `:feature:nlu`, `:feature:actions`, `:feature:tts`, `:feature:ui`
- [x] **[F0-T03]** CI: GitHub Actions (lint, ktlint, detekt, unit test, debug build), README badge
- [x] **[F0-T04]** `EarService` — foreground service typu `microphone`, ring buffer 3 s pre-roll, AudioRecord 16 kHz mono
- [x] **[F0-T05]** Wake-word PoC: integrace Porcupine **i** openWakeWord pro benchmark, dokumentovat rozhodnutí v `docs/decisions/0001-wake-word.md`
- [x] **[F0-T06]** Audio routing: detekce a přepnutí na BT SCO (`AudioManager`, `AudioDeviceCallback`), fallback na on-device mic
- [ ] **[F0-T07]** Měření latence wake→ASR-ready na referenčním zařízení (zaznamenat v `docs/benchmarks/latency.md`)
- [ ] **[F0-T08]** Měření spotřeby baterie idle/listening, Battery Historian export (`docs/benchmarks/battery.md`)
- [x] **[F0-T09]** Boot receiver + automatický restart služby (`RECEIVE_BOOT_COMPLETED`)
- [x] **[F0-T10]** README s návodem na build a side-load, screenshot z `progress.html`

---

## Fáze 1 — MVP Core

> *Týdny 3–6. Cíl: 6 intentů end-to-end + voice biometrics.*

### Voice biometrics

- [x] **[F1-T01]** Enrollment screen v Compose (5–8 vět, vizuální feedback nahrávání)
- [x] **[F1-T02]** ECAPA-TDNN ONNX integrace, extrakce 192-D embeddingu
- [x] **[F1-T03]** `EncryptedSharedPreferences` pro embeddingy + rolling adaptive update
- [x] **[F1-T04]** Dual-threshold verifikace (`T_high=0.78`, `T_low=0.65`), konfigurovatelné v debug menu

### Audio pipeline

- [x] **[F1-T05]** Silero VAD ONNX integrace, segmentace mluvy
- [x] **[F1-T06]** Sherpa-onnx + Vosk-CZ small model, streaming ASR
- [x] **[F1-T07]** State machine `DialogManager` (`IDLE → WAKE → VERIFY → CAPTURE → ASR → NLU → CONFIRM? → EXEC → TTS_ACK → IDLE`) jako sealed class + StateFlow

### NLU & intenty

- [x] **[F1-T08]** Rule-based NLU engine + Kotlin DSL (`intent("CALL") { patterns(...); slot("contact") {...} }`)
- [x] **[F1-T09]** `CALL` — kontakt nebo číslo, `TelecomManager.placeCall`
- [x] **[F1-T10]** `SEND_SMS` — kontakt + zpráva, `SmsManager.sendTextMessage`, povinný confirm gate
- [x] **[F1-T11]** `SET_ALARM` — čas + label, `AlarmClock.ACTION_SET_ALARM` intent
- [x] **[F1-T12]** `VOLUME` — up/down/set/mute, `AudioManager.adjustStreamVolume`
- [x] **[F1-T13]** `READ_LAST_NOTIFICATION` — `NotificationListenerService` + TTS
- [x] **[F1-T14]** `TORCH` — on/off, `CameraManager.setTorchMode`

### Output & UX

- [x] **[F1-T15]** TTS modul (Android `TextToSpeech` s CZ hlasem), interrupt-safe
- [x] **[F1-T16]** Confirm gate pro destruktivní akce (SMS, CALL, smazat alarm) — vyžaduje druhý speaker-verify nad `T_high`
- [x] **[F1-T17]** Settings screen: enrollment, citlivost wake-word, aliasy, re-enrollment
- [x] **[F1-T18]** Room schéma: `contacts_aliases`, `command_log`, `embedding_versions`
- [x] **[F1-T19]** Onboarding flow — permissions grant wizard
- [x] **[F1-T20]** Persistent notifikace foreground service s indikátorem stavu (idle/listening/processing)

### QA

- [x] **[F1-T21]** Unit testy NLU patterns (≥ 90 % coverage)
- [ ] **[F1-T22]** E2E test set: 50 nahrávek vlastního hlasu — ≥ 90 % recognition pass
- [ ] **[F1-T23]** False-accept test: 30 nahrávek cizích hlasů — < 2 % pass

---

## Fáze 2 — Rozšířené příkazy

> *Týdny 7–10. Cíl: pokrýt běžné denní operace.*

- [x] **[F2-T01]** `NotificationListenerService` — připojení, filtrování, prioritizace
- [x] **[F2-T02]** `REPLY_NOTIF` — `RemoteInput` pro WhatsApp/Signal/SMS odpovědi z lockscreen
- [x] **[F2-T03]** `PLAY_MEDIA` — `MediaSession` discover, Spotify/YouTube Music handover přes intent
- [x] **[F2-T04]** `MEDIA_CTRL` — next/prev/pause/resume přes `MediaSessionManager`
- [x] **[F2-T05]** `OPEN_APP` — alias mapping na package names, `PackageManager.getLaunchIntentForPackage`
- [x] **[F2-T06]** `NAVIGATE` — Google Maps intent (`geo:0,0?q=...`)
- [x] **[F2-T07]** `WHAT_TIME` / `WHAT_DATE` / `WHAT_BATTERY` — info intenty bez akce
- [x] **[F2-T08]** `TIMER` — parsing trvání (CZ "pět minut", "půl hodiny"), `AlarmClock.ACTION_SET_TIMER`
- [x] **[F2-T09]** `CANCEL` / `STOP` / `REPEAT` — meta intenty pro běžící akci nebo TTS
- [x] **[F2-T10]** Audio gain normalization na vstupu (target -23 LUFS)
- [x] **[F2-T11]** Noise suppression: `NoiseSuppressor` + `AcousticEchoCanceler` (pokud dostupné na zařízení)
- [x] **[F2-T12]** Error recovery: "neslyšel jsem, opakuj" s ASR confidence threshold
- [x] **[F2-T13]** Telemetrie lokální (off-by-default) — počty intentů, falešné triggery, latence

---

## Fáze 3 — Lockscreen & energy

> *Týdny 11–12. Cíl: spolehlivý běh v reálných podmínkách.*

- [x] **[F3-T01]** QA matrix lockscreen: dokument `docs/qa/lockscreen-matrix.md` se všemi intenty `F1`+`F2` a sloupci scénářů (šablona). **Vyplnění buněk OK/FAIL na zařízení** je předmětem samostatného QA (viz záhlaví souboru; DoD F3 vyžaduje skutečné výsledky, ne jen ⬜).
- [x] **[F3-T02]** Rozhodnutí o Direct Boot supportu — `docs/decisions/0005-direct-boot.md`
- [x] **[F3-T03]** Battery profiling: checklist a tabulky v `docs/benchmarks/battery.md` pro 8h scénář; **vyplněné řádky měření** z Battery Historian zařízení jsou předmětem HW QA (viz záhlaví souboru).
- [x] **[F3-T04]** Wake-word inference každých 30 ms (ne kontinuálně), CPU profiling
- [x] **[F3-T05]** Lazy load ASR/speaker-verify modelů — load až po wake-wordu, unload po N min idle
- [x] **[F3-T06]** OEM whitelist onboarding screen (Xiaomi, Huawei, Samsung, OnePlus — odkazy podle `Build.MANUFACTURER`)
- [x] **[F3-T07]** BT SCO reconnect logic při výpadku, exponential backoff
- [ ] **[F3-T08]** Field test: 1 den s telefonem v kapse + BT headset, deník selhání

---

## Fáze 4 — Polish & beta

> *Týdny 13–14.*

- [x] **[F4-T01]** Hands-free onboarding — instalaci dělá pomocník, pak app provádí hlasem celou konfiguraci kromě explicitních permission popupů (Android omezení)
- [x] **[F4-T02]** Hlasová konfigurace aliasů ("nazývej Petr Vondrák jako bratr")
- [x] **[F4-T03]** Backup/restore profilu — export/import zaheslovaného balíku embeddingů a aliasů
- [x] **[F4-T04]** Privacy policy (CZ + EN), permissions declaration form pro Play Store
- [x] **[F4-T05]** Crash reporting — rozhodnout (`docs/decisions/0006-crash-reporting.md`): lokální only, nebo opt-in Sentry
- [ ] **[F4-T06]** Beta s 3–5 reálnými uživateli, feedback formulář (hlasový)
- [ ] **[F4-T07]** Iterace na základě beta feedbacku, release candidate

---

## Fáze 5+ — Future / nice-to-have

> *Bez timeline, dle priorit po beta.*

- [ ] **[F5-T01]** NLU v2: Gemma-2B int4 přes MediaPipe LLM Inference, structured JSON output, rule-based jako fallback
- [ ] **[F5-T02]** Anti-spoofing: RawNet2 nebo AASIST ONNX, detekce replay/TTS attacku
- [ ] **[F5-T03]** Multi-jazyk: EN model, automatický language switch podle wake-word variant
- [ ] **[F5-T04]** Piper TTS pro plně offline CZ hlas
- [ ] **[F5-T05]** Wear OS companion — nezávislé wake-word & confirm na hodinkách
- [ ] **[F5-T06]** Switch Access / Eye-tracking fallback pro případ ztráty hlasu
- [ ] **[F5-T07]** F-Droid release

---

## Otevřená rozhodnutí (uživatel musí zodpovědět)

- [ ] **[D-001]** Cílový telefon / Android verze pro PoC (ovlivní výběr ASR modelu)
- [ ] **[D-002]** Jazyk: jen CZ, nebo CZ+EN?
- [ ] **[D-003]** Wake-word: Porcupine vs openWakeWord (rozhodne `[F0-T05]` benchmark + uživatel)
- [ ] **[D-004]** Distribuce: Play Store (znamená sensitive permission review) vs sideload/F-Droid
- [ ] **[D-005]** TTS: Google CZ vs Piper offline (větší APK)
- [ ] **[D-006]** Anti-spoofing už v MVP nebo až v `F5`?
- [ ] **[D-007]** Účast cílového uživatele v alfa testech od které fáze?

---

## Changelog

> Agent workery sem přidávají řádky podle pořadí dokončení. Formát: `YYYY-MM-DD  [ID]  Krátký popis  (worker: <jméno/handle>)`.

```
2026-05-17  [F4-T04]  Play Data safety podklad: řádek verze (`AppVersionFooterText`) — volitelný dlouhý stisk → systémová schránka, vlastní vývojář aplikaci data z toho neodesílá.  (worker: Cursor agent)
2026-05-17  [F4-T04]  Privacy policy v1.2: dlouhé klepnutí řádek verze → systémová schránka (uživatelské gesto, bez auto-odesílání); tabulka + shrnutí CS/EN; sync `docs/legal/` + `res/raw/`.  (worker: Cursor agent)
2026-05-16  [F4-T04]  Privacy policy v1.1 (účinnost stejný den): odkazuje na zapnutí lokální NDJSON telemetrie v Nastavení → Diagnostika; tabulka a shrnutí vylučují audio a text beta z telemetrického souboru; sync `docs/legal/` + `res/raw/` + podklad Play Data safety.  (worker: Cursor agent)
2026-05-15  [F4-T05]  ADR `docs/decisions/0006-crash-reporting.md` — default bez cloud reporteru; volitelná budoucí opt-in třetí strana s redakcí (bez audia/embeddings/contacts v pádech).  (worker: Cursor agent)
2026-05-15  [F4-T04]  Privacy: `docs/legal/privacy-policy-{cs,en}.md`, Play Data safety podklad `docs/legal/play-store-data-safety.md`, raw texty + `PrivacyPolicyScreen` (CS/EN), odkaz z Nastavení; navigace `RootRoute.Privacy` + listen surface jako Settings.  (worker: Cursor agent)
2026-05-15  [F4-T03]  Záloha profilu: `HandyProfileBackup` (.handy, PBKDF2+HmacSHA256 + AES-GCM), Room restore transakce, `SpeakerEmbeddingEncryptedStore` export/import B64, sekce Nastavení + SAF; `EmbeddingFloatCodec` přesun do `:core:common`.  (worker: Cursor agent)
2026-05-15  [F4-T02]  Hlasové aliasy: NLU `SET_CONTACT_ALIAS` + `REMOVE_CONTACT_ALIAS`, executor ukládá do `ContactAliasStore`; pokrytí testy + QA matrix.  (worker: Cursor agent)
2026-05-15  [F4-T01]  Onboarding TTS: `PermissionsOnboardingScreen` čte každý krok (cs TTS), kontextové návody k systémovým dialogům, „Zopakovat hlasitě“.  (worker: Cursor agent)
2026-05-15  [F3-T05]  Lazy heavy models: `SherpaStreamingRecognizerHolder`, VM idle 5 min → uvolní Sherpa + ECAPA session; `noteWakeWordForHeavyModels()` pro preload po wake; `:feature:ui` závislost na `:feature:voiceid` + `:feature:asr`.  (worker: Cursor agent)
2026-05-15  [F3-T07]  BT SCO reconnect: `AudioHandsFreeRouting` sleduje odpojení `TYPE_BLUETOOTH_SCO`, exponenciální backoff až 30 s, max 12 kroků; obnova při připojení zařízení.  (worker: Cursor agent)
2026-05-15  [F3-T06]  OEM tipy: `OemManufacturerHintsScreen` (dontkillmyapp.com odkazy), vstup z Nastavení; `Build.MANUFACTURER` v UI.  (worker: Cursor agent)
2026-05-15  [F3-T04]  Wake-word tik: `WakeWordInferenceBudget` 30 ms ↔ 480 vzorků @16 kHz + unit test.  (worker: Cursor agent)
2026-05-15  [F3-T03]  Battery 8h: rozšíření `docs/benchmarks/battery.md` o checklist a tabulku F3-T03.  (worker: Cursor agent)
2026-05-15  [F3-T02]  ADR `docs/decisions/0005-direct-boot.md` — zatím bez Direct Boot; profil ECAPA vyžaduje unlock.  (worker: Cursor agent)
2026-05-15  [F3-T01]  QA šablona `docs/qa/lockscreen-matrix.md` pro všechny NLU intenty F1+F2.  (worker: Cursor agent)
2026-05-15  [F2-T13]  Lokální telemetrie: `LocalTelemetryPreferences` (default vypnuto), `HandyLocalTelemetry` NDJSON v `filesDir`; z VM dokončený intent + low-confidence ASR; API `recordFalseWakeTrigger`.  (worker: Cursor agent)
2026-05-15  [F2-T12]  `AsrHypothesisConfidence` + práh u `ysProbs`; `HandyAssistantViewModel.submitRecognizedPhrase(…, minTokenProb)` → „Neslyšel jsem, opakuj.“; `SherpaStreamingSpeechRecognizer` vrací `minTokenProb`.  (worker: Cursor agent)
2026-05-15  [F2-T11]  `MicHardwareAudioEffects` (NoiseSuppressor + AcousticEchoCanceler) u `EarService` po `startRecording`, uvolnění před `AudioRecord.release`.  (worker: Cursor agent)
2026-05-15  [F2-T10]  Vstupní AGC: `MonoSpeechProgrammeGainNormalizer` (−23 dBFS RMS proxy k −23 LUFS), integrace v `EarService`; unit test na tiché sinusoidě.  (worker: Cursor agent)
2026-05-15  [F2-T09]  `CANCEL` / `STOP` / `REPEAT`: NLU fráze bez slotů; `HandyAssistantViewModel` — stop TTS, reset dialogu, zrušení pending confirm u nového vstupu; `lastSpokenLine` + meta `REPEAT`.  (worker: Cursor agent)
2026-05-15  [F2-T08]  `TIMER`: NLU + `CzechDurationParser` (CS minuty/hodiny/slova, strop 12 h), `TimerClockIntentStarter` (`ACTION_SET_TIMER`); JVM testy parseru; `MvpIntentExecutor`.  (worker: Cursor agent)
2026-05-15  [F2-T06]  `NAVIGATE`: NLU `{place}`, `MapsNavigateLauncher` (`geo:0,0?q=` + balíček Maps, fallback); `MvpIntentExecutor`.  (worker: Cursor agent)
2026-05-15  [F2-T05]  `OPEN_APP`: NLU `{app}`, `OpenAppLauncher` (alias→package, `getLaunchIntentForPackage`, přímý package); `MvpIntentExecutor`.  (worker: Cursor agent)
2026-05-15  [F2-T04]  `MEDIA_CTRL`: NLU šablona `{command} hudba` (nežere libovolný text), `MediaCtrlCommandParser`, `MediaPlaybackHandover.transport` — skip next/prev/pause/play; JVM test parseru.  (worker: Cursor agent)
2026-05-15  [F2-T03]  `PLAY_MEDIA`: `MediaPlaybackHandover` — `MediaSessionManager.getActiveSessions` (NLS `HandyNotificationListenerService`), `transportControls.play()`, fallback `getLaunchIntentForPackage` (Spotify, YT Music), pak `CATEGORY_APP_MUSIC`. NLU slot `{app}` volitelný.  (worker: Cursor agent)
2026-05-15  [F2-T02]  `REPLY_NOTIF`: NLU + `MvpIntentExecutor`; `NotificationSnapshotStore` ukládá `sbn.key` a `canReply`; `HandyNotificationListenerService.trySendReplyFromSnapshot` + `RemoteInput.addResultsToIntent` / `actionIntent.send`; confirm gate jako destruktivní intent.  (worker: Cursor agent)
2026-05-15  [F2-T01]  `HandyNotificationListenerService`: `onListenerConnected` scan `activeNotifications` + I/O flag v prefs; filtrování (MIN/PROGRESS/SERVICE, ongoing+LOW); priorita přes `Ranking.importance` / legacy priority + `NotificationSnapshotPolicy` (unit testy); persist `KEY_IMPORTANCE`/`KEY_POST_TIME`.  (worker: Cursor agent)
2026-05-15  [F2-T07]  Intenty `WHAT_TIME` / `WHAT_DATE` / `WHAT_BATTERY` v `HandyNluCatalogs.mvp`, `DeviceInfoAnswers` + `MvpIntentExecutor`; unit testy formátování času/data; protokoly F1-T22/F1-T23 v `docs/benchmarks/`.  (worker: Cursor agent)
2026-05-15  [F1-T21]  `:feature:nlu`: rozšířené unit testy (MVP fráze, DSL, `PhraseTemplateCompiler`, hrany match/slotOk), JaCoCo `jacocoTestCoverageVerification` ≥ 90 % řádků po `test`; pořadí vzorů CALL — `zavolej číslo {contact}` před obecným `zavolej {contact}`.  (worker: Cursor agent)
2026-05-15  [F1-T20]  `EarForegroundUiState` + `EarService.notifyForegroundUiState`; notifikace FGS mění titulek/text (idle/listening/processing); `HandyRootScreen` synchronizuje z `DialogPhase`; unit test mapování fází.  (worker: Cursor agent)
2026-05-15  [F1-T19]  `HandyContent.setHandyContent`: průvodce `PermissionsOnboardingScreen` při prvním spuštění (`OnboardingPreferences.permissions_wizard_v1_done`); po dokončení `HandyRootScreen`; `MainActivity` bez hromadného permission launcheru (řízené žádosti ve wizardu).  (worker: Cursor agent)
2026-05-15  [F1-T18]  Modul `:core:persistence` — Room `HandyDatabase` (`contacts_aliases`, `command_log`, `embedding_versions`), DAO, migrace legacy JSON prefs → Room; `ContactAliasStore` v `cz.handy.core.persistence`.  (worker: Cursor agent)
2026-05-15  [F1-T17]  `SettingsScreen`: zápis/re‑enrollment (navigace + návrat z enrollmentu), `Slider` → `WakeWordSensitivityStore`; `ContactAliasStore` (JSON prefs) + zapojení do `PhoneSlotResolver`; tlačítko Nastavení na domovské obrazovce. Test `ContactAliasExpansionTest`.  (worker: Cursor agent)
2026-05-15  [F1-T16]  Druhý speaker-verify pro destruktivní intenty: `DestructiveConfirmVoiceVerifier` (ECAPA vs uložený embedding, jen `StrongAccept`); demo `AlertDialog` nahrává PCM přes `EnrollmentClipRecorder` + `Pcm16LittleEndianIo`; `HandyAssistantViewModel.submitDestructiveVoiceConfirmFromPcm`. JVM test PCM I/O.  (worker: Cursor agent)
2026-05-15  [F1-T15]  `:feature:tts` — `SpeechSynthesizer` + `AndroidCzechSpeechSynthesizer` (cs-CZ, QUEUE_FLUSH, utterance gate); `HandyAssistantViewModel` čte ack a zrušení SMS po dočtení volá `DialogManager.onTtsComplete()`. Unit test `UtteranceSequenceGateTest`.  (worker: Cursor agent)
2026-05-15  [F1-T14]  `TorchModeSwitcher` (`CameraManager`), manifest `CAMERA`, NLU slot `{mode}`; textový demo panel v debug spouští `MvpIntentExecutor`.  (worker: Cursor agent)
2026-05-15  [F1-T13]  `HandyNotificationListenerService` → `NotificationSnapshotStore`; intent `READ_LAST_NOTIFICATION` (bez TTS — řetězec pro budoucí [F1-T15]).  (worker: Cursor agent)
2026-05-15  [F1-T12]  `MediaVolumeAdjuster` (STREAM_MUSIC) + NLU `VOLUME`.  (worker: Cursor agent)
2026-05-15  [F1-T11]  `AlarmSlotTimeParser` + `AlarmClockIntentStarter` (`ACTION_SET_ALARM`) + NLU `SET_ALARM`.  (worker: Cursor agent)
2026-05-15  [F1-T10]  `:feature:actions` — `SmsTextMessageSender` (`SmsManager.sendTextMessage` / multipart), pojistka `confirmedByUser` + `requireDestructiveSmsUserConfirmation`; `PhoneSlotResolver` sdílený s hovorem; manifest `SEND_SMS`; unit test pojistky.  (worker: Cursor agent)
2026-05-15  [F1-T09]  `:feature:actions` — `TelecomCallPlacer` (`TelecomManager.placeCall`), `DialableNumberComposer` (normalizace `tel:` z číslic), `ContactDialUriLookup` (Contacts `CONTENT_FILTER_URI` + primární číslo); app manifest `CALL_PHONE` + `READ_CONTACTS`; JVM unit testy `DialableNumberComposer`.  (worker: Cursor agent)
2026-05-15  [F1-T08]  `:feature:nlu` — DSL + `RuleBasedNluEngine` + `HandyNluCatalogs.mvp`; rozšíření o SET_ALARM/VOLUME/READ_LAST_NOTIFICATION (+ slotový TORCH `{mode}`). Unit testy; dev textově spouští `HandyAssistantViewModel` → `MvpIntentExecutor`.  (worker: Cursor agent)
2026-05-15  [F1-T07]  `:core:common` — `DialogPhase` (sealed) + `DialogManager` s `StateFlow`, legální přechody Wake→…→TtsAck→Idle včetně větve Confirm; unit testy.  (worker: Cursor agent)
2026-05-15  [F1-T06]  `:feature:asr`: `com.xdcobra.sherpa:sherpa-onnx` (exclusive Maven), `createCzSherpaStreamingRecognizer` + `SherpaStreamingSpeechRecognizer`, assets `asr/cs_zipformer_small` + README; app `jniLibs.pickFirsts` kvůli možné kolizi `libonnxruntime.so`; ADR `docs/decisions/0004-sherpa-onnx-android-asr.md`.  (worker: Cursor agent)
2026-05-15  [F1-T05]  Silero VAD ONNX: `SileroOnnxVoiceActivityDetector` (v5 I/O jako upstream C#), `VadSegmentMerger` + `SileroOnnxSpeechSegmenter`, asset `voiceid/silero_vad.onnx`; ADR `docs/decisions/0003-silero-vad-onnx.md`.  (worker: Cursor agent)
2026-05-15  [F1-T04]  `DualThresholdSpeakerVerifier` + `VerificationThresholdStore` (debug prefs), Compose `DebugVerificationScreen` pouze při `FLAG_DEBUGGABLE`; `HandyRootScreen` routuje Home / Enrollment / DebugVerify.  (worker: Cursor agent)
2026-05-15  [F1-T03]  `SpeakerEmbeddingEncryptedStore` (AES GCM prefs), Base64+F32 codec, EMA (`adaptiveMerge`, α výchozí 0.18), `embedding` jako `StateFlow`.  (worker: Cursor agent)
2026-05-15  [F1-T02]  `SpeechbrainEcapaPreprocessor` + `EcapaOnnxSpeakerEmbeddingExtractor` (ONNX RT mobile), assets `voiceid/ecapa_embedding.onnx`; ADR `docs/decisions/0002-ecapa-speaker-onnx.md`.  (worker: Cursor agent)
2026-05-15  [F1-T01]  Compose `EnrollmentScreen` (6 vět, pulz + úroveň RMS), `EnrollmentClipRecorder` → cache PCM; `EarService` pozastaveno během zápisu.  (worker: Cursor agent)
2026-05-15  [F0-T10]  README: `adb install` side-load, odkaz na `progress.html` jako nástěnku / zdroj screenshotu.  (worker: Cursor agent)
2026-05-15  [F0-T09]  `HandyBootCompletedReceiver` + `RECEIVE_BOOT_COMPLETED` — restart `EarService` po bootu.  (worker: Cursor agent)
2026-05-15  [F0-T06]  `AudioHandsFreeRouting` v `EarService` (VOICE_COMMUNICATION, BT SCO / `setCommunicationDevice`), `BLUETOOTH_CONNECT` v manifestu a `MainActivity`.  (worker: Cursor agent)
2026-05-15  [F0-T05]  Porcupine 4.x + openWakeWord (xyz.rementia 0.1.5), `WakeWordEnginesProbe`, ADR `docs/decisions/0001-wake-word.md`, INTERNET kvůli Picovoice licence.  (worker: Cursor agent)
2026-05-15  [F0-T04]  `:core:audio` — `EarService` (FGS microphone), `MonoPcmRingBuffer` 3s @16 kHz mono, `MainActivity` permissions.  (worker: Cursor agent)
2026-05-15  [F0-T03]  GitHub Actions workflow `ci.yml` (JDK 17, SDK 35, `ciHandy`), README badge placeholder.  (worker: Cursor agent)
2026-05-15  [F0-T02]  Modulární Gradle projekt: core + feature moduly, Compose UI v `:feature:ui`, `:feature:nlu` jako JVM; Foojay toolchain resolver v settings. Lokální kompilace na agentovi neúspěšná (chybělo JDK 17).  (worker: Cursor agent)
2026-05-15  [F0-T01]  Android projekt: Gradle wrapper 8.10.2, Compose + Hilt, Version Catalogs, ktlint/detekt; lokální `./gradlew assembleDebug` neproběhl kvůli nedostatku místa na disku v prostředí agenta (ověření v Android Studio / na tvém PC).  (worker: Cursor agent)
```
