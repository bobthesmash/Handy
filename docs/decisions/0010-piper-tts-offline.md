# ADR-0010: Piper TTS plně offline (F5-T04) — zatím dokumentace + asset kontrakt

## Stav

Accepted (2026-05-17)

## Kontext

Uživatelé chtějí číst odpovědi bez závislosti na Google TTS. Piper nabízí malé ONNX modely lokálně; narážíme na velikost APK a nutnost audio výstupu (JNI / AudioTrack).

## Rozhodnutí

- **v1 výchozí zůstává** Android `TextToSpeech` cs-CZ ([ADR-0009](./0009-product-direction-defaults.md)).
- **Asset kontrakt** dokumentujeme v `feature/tts/src/main/assets/piper/README.txt`; žádný ONNX Piper v repozitáři.
- **Nativní přehrávací vrstva** přijde v samostatné PR až po výběru konkrétního Piper build pipeline (bez cloud).

## Důsledky

- `[F5-T04]` je splněný z pohledu „cesta a právní rámec existuje“; finální přehrávání vyžaduje dodatečný kód mimo tento ADR.

## Související

- `SpeechSynthesizer` v `:feature:tts`
- `README.md` uživatelský build
