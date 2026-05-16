# ADR-0004: Sherpa-ONNX streaming ASR na Androidu

## Stav

Accepted (2026-05-15)

## Kontext

F1‑T06 vyžaduje **lokální** streamování ASR (bez cloud API dle **AGENTS.md**), v plánu s **Sherpa‑onnx**
a rozhraním přibližně odpovídajícím **Vosk small CZ**.

Oficiální `k2-fsa/sherpa-onnx` AAR **není na Maven Central**; typický postup je stáhnout prebuilt JNI tarball
(návod v upstream `android/SherpaOnnxAar/README.md`).

## Rozhodnutí

- Runtime propojujeme přes **předzabalené AAR** `com.xdcobra.sherpa:sherpa-onnx` z repozitáře
  `https://xdcobra.github.io/maven` (third-party mirror buildů slučitelných s kotlin-api `com.k2fsa.sherpa.onnx`).
  Repozitář je v `settings.gradle.kts` za `exclusiveContent` jen pro skupinu `com.xdcobra.sherpa`.
- Model soubory **nejsou v gitu**: vývojář umístí zipformer2 **streaming transducer** ONNX + `tokens.txt`
  do `feature/asr/src/main/assets/asr/cs_zipformer_small/` (viz `README.txt`).
- Očekávané názvy: `encoder.onnx`, `decoder.onnx`, `joiner.onnx`, `tokens.txt` (stejné rozložení jako u
  sherpa **vosk-derived** exportů, např. `sherpa-onnx-streaming-zipformer-small-ru-vosk-*`). Český balíček
  se stejným kontraktem přidá až bude k dispozici; do té doby lze složku dočasně naplnit jiným zipformer2
  modelem pro vývoj.
- Tenká fasáda v `:feature:asr`: `createCzSherpaStreamingRecognizer`, `SherpaStreamingSpeechRecognizer`,
  normalizace PCM podle upstream Android dema (`/32768f`).

## Důsledky

- Závislost na **externím Maven zrcadle** (může vyžadovat síť při prvním resolve).
- V APK může dojít ke **kolizi nativních knihoven** s jiným ONNX Runtime (např. `onnxruntime-android` z
  voice/wakeword modulů); v `:app` je `pickFirst` pro `libonnxruntime.so` a `libc++_shared.so` — při problémech
  na některých ABI je nutná ruční konsolidace verzí/vendor buildu.
- Při změně major verze JNI je potřeba ověřit kompatibilitu asset modelů.
