# Handy

**On-device voice assistant for Android.** Kotlin. The microphone stream never leaves the phone.

This is a public work-in-progress of a full speech stack I run on a Galaxy S23: foreground mic service, streaming ASR, local NLU, speaker-check, TTS, and device actions. It is **not** a Play Store product and **will not speak after a bare clone** — model weights stay out of git on purpose.

| You want | Go here |
| --- | --- |
| Thirty-second scan | [Why this exists](#why-this-exists) |
| What actually runs today | [State of the demo](#state-of-the-demo) |
| How the pipeline is wired | [Architecture](#architecture) |
| Build / sideload | [Run it](#run-it) |
| Models, keys, what is *not* in git | [What stays off GitHub](#what-stays-off-github) |

---

## Why this exists

Most “AI assistants” on a phone are a thin client around someone else’s cloud. Handy is the opposite: a **local pipeline** with hard edges.

- **Privacy as a constraint, not a slogan.** PCM is captured in a microphone foreground service, held in a 3-second ring buffer, and consumed on-device. There is no speech-to-cloud API in the path.
- **A real Android problem, not a tutorial.** Wake, ASR, NLU, speaker verify, and actions have to coexist with Samsung battery policy, a lock screen, and a TTS echo that will happily command itself.
- **Evidence I can hold a messy system.** SaxSmith shows I can ship a web app. Handy is the piece I want a hiring loop to open: JNI, ONNX Runtime, streaming transducers, and Kotlin modules that still compile under CI.

If you are skimming for a hire: start at `core/audio` → `feature/asr` → `feature/nlu` → `feature/actions`. If you want the product story, stay on this page.

---

## State of the demo

Honest snapshot (September 2026), Galaxy S23 (`SM-S911B`):

**Works (demonstrated on device)**

- Debug APK sideloads; the old `OrtGetApiBase` crash is gone (ONNX Runtime 1.25).
- `EarService` keeps a microphone foreground notification.
- Streaming Sherpa-ONNX ASR is alive.
- End-to-end: spoken **“what time is it”** → NLU `WHAT_TIME` → English spoken answer, including with the screen off.
- Local intent catalog also knows battery, date, flashlight, timer, unlock, plus Czech originals (call / SMS / maps). Hit-rate on noisy ASR is still the weak joint.

**Does not work yet (do not pretend)**

- There is **no reliable “Handy” wake word** in this tree. Picovoice is optional and **not required** to read or compile the code. A Picovoice key is not in the repo. Gmail / university mail cannot register on their console anyway. OpenWakeWord assets (`hey_handy.onnx` and friends) are **not** checked in.
- Closing the UI used to leave the mic up (sticky FGS). The public branch stops the ear on back / task-removed / `onDestroy`. Confirm on a phone before you trust it.
- Always-on listen still lives partly in the Activity `ViewModel`. The correct home is the ear service. That move is next, not done.
- There is **no Czech ASR model** in git. The `cs_zipformer_small` slot is a filename from the product target. A public clone without weights will skip recognizer init.
- This is not a Play listing, not a medical device, not a cloud assistant.

---

## Architecture

```
mic  →  EarService (FGS, 16 kHz mono ring)
     →  EarAudioBridge
     →  wake (optional)  |  streaming ASR (Sherpa zipformer)
     →  NLU (compiled phrase templates + rule engine)
     →  speaker gate (ECAPA, only when a profile exists)
     →  MvpIntentExecutor  →  torch / time / battery / calls / …
     →  TTS (device engine)
```

| Stage | Module | Read this first |
| --- | --- | --- |
| Capture | `core/audio` | [`EarService.kt`](core/audio/src/main/kotlin/cz/handy/core/audio/EarService.kt) |
| ASR | `feature/asr` | [`CzZipformerSherpaAssets.kt`](feature/asr/src/main/kotlin/cz/handy/feature/asr/CzZipformerSherpaAssets.kt) |
| NLU | `feature/nlu` | [`HandyNluCatalogs.kt`](feature/nlu/src/main/kotlin/cz/handy/feature/nlu/HandyNluCatalogs.kt) |
| Actions | `feature/actions` | [`MvpIntentExecutor.kt`](feature/actions/src/main/kotlin/cz/handy/feature/actions/executor/MvpIntentExecutor.kt) |
| Voice ID | `feature/voiceid` | ECAPA embedding + Silero VAD (weights not in git) |
| Wake | `feature/wakeword` | Architecture only until you drop in your own models |
| UI / dialog | `feature/ui` | [`HandyAssistantViewModel.kt`](feature/ui/src/main/kotlin/cz/handy/feature/ui/pipeline/HandyAssistantViewModel.kt) |

Modules: `:app` `:core:common` `:core:audio` `:core:persistence` `:feature:wakeword` `:feature:asr` `:feature:voiceid` `:feature:nlu` `:feature:actions` `:feature:tts` `:feature:ui`.

<details>
<summary>Design choices worth arguing about</summary>

- **One `AudioRecord`, many readers.** Wake and ASR share `EarAudioBridge` instead of opening a second capture session (Samsung will otherwise fight you).
- **NLU is data, not `if (text.contains)` soup.** Phrase templates compile to regex with ordered slots (`PhraseTemplateCompiler`). First match wins, so the catalog order is part of the spec.
- **Destructive intents confirm.** `CALL` / `SEND_SMS` / `SET_ALARM` require confirm. Speaker-verify can gate them when a centroid exists.
- **TTS is muted out of ASR.** The assistant will otherwise transcribe “it is three p.m.” and fire `REPEAT`. The ViewModel drops mic into Sherpa while speaking.
- **Models are a download, not a commit.** A 292 MB encoder in git is a trap for anyone who clones, and a gift to nobody.

</details>

---

## Run it

**You need**

- JDK 17+
- Android SDK (API 35 / build-tools 35)
- `local.properties` with `sdk.dir=...` (see `local.properties.example` if present)
- Optional: your own ONNX weights under `feature/asr/src/main/assets/asr/cs_zipformer_small/` named `encoder.onnx` `decoder.onnx` `joiner.onnx` `tokens.txt`
- Optional: ECAPA / Silero under `feature/voiceid/src/main/assets/voiceid/`
- Do **not** commit those files

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

CI entrypoints on a machine with the SDK:

```bash
./gradlew ciHandy        # checks, ktlint, unit tests
./gradlew ciHandyFull    # plus release / R8
```

**On a Samsung:** Settings → Apps → Handy → Battery → **Unrestricted**, or the ear service will look alive and hear nothing once the screen sleeps.

Spoken phrases the current catalog accepts in English (exact-ish, after punctuation strip): `what time is it`, `battery level`, `what date is it`, `turn on flashlight`, `turn off flashlight`. Czech originals for calls, SMS, navigation, volume still live in the same catalog.

---

## What stays off GitHub

| Left out | Why |
| --- | --- |
| `*.onnx` `*.tflite` `*.ppn` | Weights and wake keywords. Too big, too easy to leak, useless without a license you own |
| `local.properties` | SDK path and any `picovoice.access.key` |
| Keystores / `google-services.json` | Signing and vendor secrets |
| `sherpa.tar.bz2`, `_onnx_backup/`, unpacked zipformer dumps | Local cache from bringing ASR up. Not source |
| Crash logs, `DEBUG_STATUS.md` | Device diaries, not the product |

If a file is a secret or a 100 MB blob, it does not belong in a `git add -A`. The `.gitignore` in this branch is written for that.

---

## License

Apache License 2.0. See [LICENSE](LICENSE).
