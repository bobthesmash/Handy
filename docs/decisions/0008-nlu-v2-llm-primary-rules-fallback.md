# ADR-0008: NLU v2 — strukturovaný LLM jako primární záležitost, pravidlový engine jako záloha

## Stav

Accepted (implementováno v repu včetně volitelného MediaPipe `.task`, 2026-05-17)

## Kontext

`[F5-T01]` požaduje **Gemma‑2B int4 přes MediaPipe LLM Inference** s strukturovaným výstupem a **fallback na pravidlové NLU** (`RuleBasedNluEngine`, `[F1-T08]`). Cloud API je vyloučené pro jádro (**`AGENTS.md`**).

## Rozhodnutí

- **Inferenční pořadí**: nejprve parser z řetězce LLM (JSON / struktura mapovatelný na [`ParsedIntent`](../feature/nlu/src/main/kotlin/cz/handy/feature/nlu/ParsedIntent.kt)), výsledek `NluResult.NoMatch` (viz totéž místo) předává řízení **stejnému** rule enginu jako dnes.
- **Implementace v repu (fáze 1)**:
  - Rozhraní [`UtteranceNluParser`](../feature/nlu/src/main/kotlin/cz/handy/feature/nlu/UtteranceNluParser.kt),
  - [`LlmPrimaryRuleFallbackNluEngine`](../feature/nlu/src/main/kotlin/cz/handy/feature/nlu/LlmPrimaryRuleFallbackNluEngine.kt) skládá oba vstupy,
  - [`UnbundledLlmNluParser`](../feature/nlu/src/main/kotlin/cz/handy/feature/nlu/UnbundledLlmNluParser.kt) je zástupce vracející vždy `NoMatch`, dokud není aktivní MediaPipe řetězec a lokální váhy v assets/app storage.
  - [`HandyAssistantViewModel`](../feature/ui/src/main/kotlin/cz/handy/feature/ui/pipeline/HandyAssistantViewModel.kt) používá tento chain — chování s prázdným LLM parserem je **identické** s výhradně pravidlovým NLU.

**Fáze 2**: závislost **`com.google.mediapipe:tasks-genai`** v modulu `:feature:nlu-llm`, soubor `gemma_hand_task.task` v `assets/nlu_llm/` (viz README v assets); bez souboru jsme funkčně na pravidlech + JSON z textu (testy / ruční vložení).

## Důsledky

- Až bude LLM špatně generovat / halucinovat, rule engine zůstává deterministickou zálohou.
- Velikost APK a údržba NDK/MediaPipe sady jsou oddělené od „logické“ skladby NLU (lze testovat chain samostatně).
- Bez fáze 2 se **nepočítá** s nižší latencí ani jinými vlastnostmi LLM — úkol `[F5-T01]` zůstává otevřený do hotové inference.

## Související

- [`0002-ecapa-speaker-onnx.md`](./0002-ecapa-speaker-onnx.md) — precedens pro binárky mimo git.
- [`0007-anti-spoofing-onnx.md`](./0007-anti-spoofing-onnx.md) — další ONNX/váhy ve F5.
