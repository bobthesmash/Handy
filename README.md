# Handy

[![CI](https://github.com/bobthesmash/Handy/actions/workflows/ci.yml/badge.svg)](https://github.com/bobthesmash/Handy/actions/workflows/ci.yml)

Lokální hlasový asistent pro Android (asistivní ovládání, biometrie hlasem, offline provoz). Stav vývoje: `progress.html`.

## Požadavky

- JDK **17+**
- Android SDK (API **35** platforma, build-tools **35**)
- Projekt používá Gradle Wrapper (`./gradlew`)

### Referenční HW (měření / QA)

- **Samsung Galaxy S20** (`[D-001]` v `IMPLEMENTATION_PLAN.md`) — výchozí telefon pro latenci (`docs/benchmarks/latency.md`), baterii (`docs/benchmarks/battery.md`), lockscreen matrix (`docs/qa/lockscreen-matrix.md`) a field test. Přesnou verzi Androidu / One UI zapisovat při prvních měřeních do příslušných tabulek.

## Build

```bash
chmod +x gradlew   # jen na Linux/macOS
./gradlew ciHandy
```

Nebo jen APK:

```bash
./gradlew :app:assembleDebug
```

### Side-load (ADB)

Po úspěšném `:app:assembleDebug` je APK typicky:

`app/build/outputs/apk/debug/app-debug.apk`

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Nebo jen přetáhni APK do zařízení a otevři ho v souborové aplikaci (u některých OEM povolit „instalovat z neznámých zdrojů“).

### Přehled vývoje

Soubor **`progress.html`** je lokální stavová nástěnka — otevři ho v prohlížeči; můžeš z něj pořídit screenshot pro dokumentaci nebo prezentaci.

### Odznak CI

Soukromý remote: [`bobthesmash/Handy`](https://github.com/bobthesmash/Handy) — badge výše po prvním úspěšném běhu Actions.

### Ochrana soukromí a Play Store (F4-T04)

- **`docs/legal/privacy-policy-cs.md`** a **`docs/legal/privacy-policy-en.md`** — plné znění zásad.
- **`docs/legal/play-store-data-safety.md`** — orientační podklad pro sekci *Data safety* v Google Play Console.
- V aplikaci: **Nastavení → Zásady ochrany soukromí** (`PrivacyPolicyScreen`, zkrácený text z `res/raw`).

### Wake word & Picovoice (F0-T05)

- **Produkční runtime:** Porcupine nad mono PCM ze sdíleného audio mostu (`EarAudioBridge` → `PorcupineEarWakePump`); při detekci se volá `noteWakeWordForHeavyModels()` pro spuštění těžších modelů. Vyžaduje `picovoice.access.key` (viz níže).
- **openWakeWord (experiment):** ONNX váhy do `feature/wakeword/src/main/assets/openwakeword/` — viz **`README.txt`** v té složce; bez souborů zůstane jen Porcupine / benchmark (`WakeWordEnginesProbe`). Rozhodnutí a omezení v **`docs/decisions/0001-wake-word.md`**.
- **`local.properties`** (necommitovat):
  ```properties
  picovoice.access.key=VÁŠ_KLÍČ_Z_PICVOICE_CONSOLE
  ```
  Bez něj přeskočí debug benchmark `WakeWordEnginesProbe` (build zůstane zelený).
- **`INTERNET` permission**: podle dokumentace Picovoice kvůli **validaci přístupového klíče**. Inference mikrofonu zůstává na zařízení — viz **`docs/decisions/0001-wake-word.md`**.
- **openWakeWord binding:** Maven `xyz.rementia:openwakeword`; výchozí očekávaný klasifikátor `hey_handy.onnx` — přejmenuj nebo uprav `OpenWakeWordEngineFactory`.

### Voice biometrics (ECAPA + VAD)

- **`feature/voiceid/src/main/assets/voiceid/`** — **`README.txt`**: `ecapa_embedding.onnx` a `silero_vad.onnx` (Silero VAD v5 ONNX pro segmentaci řeči); oba záměrně mimo git kvůli velikosti. ADR `docs/decisions/0002-ecapa-speaker-onnx.md`, `docs/decisions/0003-silero-vad-onnx.md`.

### ASR (Sherpa‑onnx streaming)

- **JNI / Java API**: `com.xdcobra.sherpa:sherpa-onnx` — verze alias **`sherpaOnnx`** v **`gradle/libs.versions.toml`** (Maven `https://xdcobra.github.io/maven`, jen skupina `com.xdcobra.sherpa`; viz **`docs/decisions/0004-sherpa-onnx-android-asr.md`**).
- **`feature/asr/src/main/assets/`** — viz **`README.txt`**: ONNX zipformer2 transducer (+ `tokens.txt`) do `asr/cs_zipformer_small/`.

## Licence

(doplní vlastník repozitáře)
