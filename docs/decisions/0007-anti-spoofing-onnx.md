# ADR-0007: Anti-spoofing (replay / synth) jako volitelný ONNX nad stejným log-melem jako ECAPA

## Stav

Accepted (2026-05-17)

## Kontext

`[F5-T02]` cílí na **replay / TTS útok** proti řetězi speaker-verify. Repo už počítá **SpeechBrain‑kompatibilní log-mel** v [`SpeechbrainEcapaPreprocessor`](../../feature/voiceid/src/main/kotlin/cz/handy/feature/voiceid/ecapa/SpeechbrainEcapaPreprocessor.kt); ECAPA ONNX ho používá ([ADR‑0002](./0002-ecapa-speaker-onnx.md)). Cloud ani upload audia zakazuje **`AGENTS.md`**.

## Rozhodnutí

- **Soubor nepovinný**: `anti_spoof.onnx` v `feature/voiceid/src/main/assets/voiceid/`. Bez něj aplikace **přeskakuje** anti-spoof a zůstává jen kosínová ECAPA brána.
- **Inferenční pořadí**: před načtením embeddingu uloženého profilu a před kosínovým skóre se volá **`AntiSpoofOnnxClassifier.gateBeforeSpeakerVerify`** (stejná cesta jako brána před NLU a druhý krok u SMS/hovoru/alarmů).
- **Vstupní kontrakt** (aligned s ECAPA ONNX):
  - float32 **`[1, T, 80]`** nebo **`[1, 80, T]`** (auto-detekce jako u ECAPA),
  - `T` časové rámce, `80` melů ze stejného preprocessoru jako ECAPA (**včetně sentence‑mean nad rámci** při současném kódu).
- **První výstup tenzoru řezy do `FloatArray` a interpretace**:
  - **1 hodnota** → `sigmoid(x)` jako **P(spoof)** ∈ (0,1),
  - **≥ 2 hodnoty** → softmax přes **`[bonafide, spoof]`** = první dva prvky výstupu (**P(spoof)** = softmax druhého logitu).
  - Jiné exporty uživatel nesladí bez úpravy kódu nebo exportní hlavy — repo záměrně drží jen tento rozumný základ.
- **Rozhodování**: lokálně uložený práh **`antiSpoofRejectAbove`** (`VerificationThresholds.DEFAULT_ANTI_SPOOF_REJECT_ABOVE` = **0.5**). Pokud **P(spoof) > práh**, tah se zamítne (**fail-closed k útoku**, ne k dostupnosti modelu).
- **Chyba inference přítomého modelu** (session / shape / prázdny výstup): **`AntiSpoofInferenceException`** — uživatel neprojde řetězcem ani při bezchybném ECAPA (fail-closed vůči poškozenému buildu ONNX).

## Důsledky

- Vývojář musí **exportovat ONNX** tak, aby vstupně-výstupní hlava seděla výše — typicky vlastní distillace/adaptace existujícího RawNet/AASIST těla nelze jen „hodit sem“ bez měření, pokud očekává jiné akustické rysy (CQCC LFCC …).
- **APK bez `anti_spoof.onnx`**: bezpečnostní vlastnost je stejná jako před adopcí této vrstvy (pouze ECAPA).
- **Telemetrie** (pokud je zapnutá): `anti_spoof_gate` + stávající `speaker_phrase_gate` s outcomes `blocked_anti_spoof` / `anti_spoof_onnx_error`.

## Související

- Kod: [`AntiSpoofOnnxClassifier`](../../feature/voiceid/src/main/kotlin/cz/handy/feature/voiceid/antispoof/AntiSpoofOnnxClassifier.kt), zapojení v [`DestructiveConfirmVoiceVerifier`](../../feature/voiceid/src/main/kotlin/cz/handy/feature/voiceid/confirm/DestructiveConfirmVoiceVerifier.kt).
