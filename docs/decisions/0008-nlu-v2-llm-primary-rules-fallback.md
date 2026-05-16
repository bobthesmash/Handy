# ADR-0008: NLU v2 — strukturovaný LLM jako primární záležitost, pravidlový engine jako záloha

## Stav

Accepted (draft implementace řetězce, 2026-05-17)

## Kontext

`[F5-T01]` požaduje **Gemma‑2B int4 přes MediaPipe LLM Inference** s strukturovaným výstupem a **fallback na pravidlové NLU** (`RuleBasedNluEngine`, `[F1-T08]`). Cloud API je vyloučené pro jádro (**`AGENTS.md`**).

## Rozhodnutí

- **Inferenční pořadí**: nejprve parser z řetězce LLM (JSON / struktura mapovatelný na [`ParsedIntent`](../feature/nlu/src/main/kotlin/cz/handy/feature/nlu/ParsedIntent.kt)), výsledek `NluResult.NoMatch` (viz totéž místo) předává řízení **stejnému** rule enginu jako dnes.
- **Implementace v repu (fáze 1)**:
  - Rozhraní [`UtteranceNluParser`](../feature/nlu/src/main/kotlin/cz/handy/feature/nlu/UtteranceNluParser.kt),
  - [`LlmPrimaryRuleFallbackNluEngine`](../feature/nlu/src/main/kotlin/cz/handy/feature/nlu/LlmPrimaryRuleFallbackNluEngine.kt) skládá oba vstupy,
  - [`UnbundledLlmNluParser`](../feature/nlu/src/main/kotlin/cz/handy/feature/nlu/UnbundledLlmNluParser.kt) je zástupce vracející vždy `NoMatch`, dokud není aktivní MediaPipe řetězec a lokální váhy v assets/app storage.
  - [`HandyAssistantViewModel`](../feature/ui/src/main/kotlin/cz/handy/feature/ui/pipeline/HandyAssistantViewModel.kt) používá tento chain — chování s prázdným LLM parserem je **identické** s výhradně pravidlovým NLU.

**Fáze 2** (bez data tohoto ADR automaticky nevynucená konkrétní verzí): přidání závislosti **`com.google.mediapipe:tasks-genai`** (nebo tehdy dokumentovaného ekvivalentu), lokálních vah Gemma int4 mimo repozitář, parsování kontraktu JSON → `ParsedIntent` včetně validace `intentId` proti katalogu nebo bezpečného podmnoží.

## Důsledky

- Až bude LLM špatně generovat / halucinovat, rule engine zůstává deterministickou zálohou.
- Velikost APK a údržba NDK/MediaPipe sady jsou oddělené od „logické“ skladby NLU (lze testovat chain samostatně).
- Bez fáze 2 se **nepočítá** s nižší latencí ani jinými vlastnostmi LLM — úkol `[F5-T01]` zůstává otevřený do hotové inference.

## Související

- [`0002-ecapa-speaker-onnx.md`](./0002-ecapa-speaker-onnx.md) — precedens pro binárky mimo git.
- [`0007-anti-spoofing-onnx.md`](./0007-anti-spoofing-onnx.md) — další ONNX/váhy ve F5.
