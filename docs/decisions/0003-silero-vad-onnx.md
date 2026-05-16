# ADR-0003: Silero VAD přes ONNX Runtime (segmentace řeči)

## Stav

Accepted (2026-05-15)

## Kontext

Pro F1‑T05 potřebujeme **lokální** detekci řeči / ticha pro segmentaci vstupu před ASR a biometrií.

Cloud API ani posílání audia je vyloučené (**AGENTS.md**).

## Rozhodnutí

- Používáme **Silero VAD v5** v exportu **ONNX**, se stejným enginem jako ECAPA (**onnxruntime-android**; konzistence s ADR‑0002).
- Model **`silero_vad.onnx`** se **nebude commitovat** — vývojář jej vloží vedle ECAPA do `feature/voiceid/src/main/assets/voiceid/` (viz `README.txt`).
- Inferenční kontrakt sleduje oficiální referenci **C# / ONNX** (`input` `[1,576]` float = 64 kontext + 512 nových vzorků, `sr` int64 skalár, `state` `[2,1,128]`; výstupy `output`, `stateN`).
- Segmentace nad pravděpodobnostmi: **VadSegmentMerger** s hysterezí (`thresholdOn` / `thresholdOff`), minimální délka řeči ~160 ms a koncové ticho ~120 ms (vzorky @ 16 kHz).

## Důsledky

- Další ONNX váha v APK (stejná distribuční politika jako ECAPA).
- Při změně exportu (jiné názvy I/O nebo topologie) je nutné upravit `SileroOnnxVoiceActivityDetector` nebo znovu exportovat model v souladu se Silero v5 ONNX.
