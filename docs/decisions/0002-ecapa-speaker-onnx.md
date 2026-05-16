# ADR-0002: ECAPA‑TDNN speaker embedding přes ONNX Runtime na zařízení

## Stav

Accepted (2026-05-15)

## Kontext

Pro F1‑T02 je potřeba extrahovat **192‑dim** embedding z řeči **offline**. Projekt zakazuje cloud API pro jádrové funkce (**AGENTS.md**).

## Rozhodnutí

- Používáme **ONNX Runtime for Android** (`com.microsoft.onnxruntime:onnxruntime-android`) jako jednotný inference engine i pro budoucí ONNX modely (wake word již ONNX zmiňuje v ADR-0001).
- Model **nebude součástí repozitáře**: vývojář vloží `ecapa_embedding.onnx` do `feature/voiceid/src/main/assets/voiceid/`.
- Předfiltrování: **SpeechBrain‑kompatibilní log‑mel (80 ks)** počítaný v Kotlinu (**`SpeechbrainEcapaPreprocessor`**), aby export z Hugging Face / SpeechBrain používal stejný vstupní tensor jako trénink (viz parametry ECAPA pretrained).
- Embedding na výstupu se normalizuje **L2** pro kosínové skórování v dalších úkolech (F1‑T03/F1‑T04).

## Důsledky

- APK roste o váhu souboru v assets (závislost na vlastní distribuci modelu bez uploadu uživatelské řeči).
- Export ONNX musí být v souladu se vstupem mel (80): při vlastní změně topologie je nutná úprava preprocessing nebo ONNX.
