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
  - [`UnbundledLlmNluParser`](../feature/nlu/src/main/kotlin/cz/handy/feature/nlu/UnbundledLlmNluParser.kt) je zástupce vždy `NoMatch` (zůstává pro unit testy a případné „vypnutí“ primární vrstvy).
  - [`StructuredJsonUtteranceLlmParser`](../feature/nlu/src/main/kotlin/cz/handy/feature/nlu/StructuredJsonUtteranceLlmParser.kt): pokud vstup (např. ASR) začíná `{`, parsuje přes [`LlmNluJsonCodec`](../feature/nlu/src/main/kotlin/cz/handy/feature/nlu/LlmNluJsonCodec.kt); jinak `NoMatch` → pravidla.
  - [`LlmNluJsonCodec`](../feature/nlu/src/main/kotlin/cz/handy/feature/nlu/LlmNluJsonCodec.kt) parsuje JSON výstup modelu na `NluResult` s validací slotů vůči [`IntentCatalog`](../feature/nlu/src/main/kotlin/cz/handy/feature/nlu/IntentCatalogDsl.kt) (bez síťového volání).
  - [`HandyAssistantViewModel`](../feature/ui/src/main/kotlin/cz/handy/feature/ui/pipeline/HandyAssistantViewModel.kt) používá `LlmPrimaryRuleFallbackNluEngine(StructuredJsonUtteranceLlmParser, RuleBasedNluEngine)` se **stejnou** instancí [`HandyNluCatalogs.mvp`](../feature/nlu/src/main/kotlin/cz/handy/feature/nlu/HandyNluCatalogs.kt). Běžná mluvená věta → **identické** chování jako čistě pravidlové NLU; JSON řetězec v textu → primární vrstva může vrátit `Matched` dřív než pravidla.

**Fáze 2** (bez data tohoto ADR automaticky nevynucená konkrétní verzí): přidání závislosti **`com.google.mediapipe:tasks-genai`** (nebo tehdy dokumentovaného ekvivalentu), lokálních vah Gemma int4 mimo repozitář, parsování kontraktu JSON → `ParsedIntent` včetně validace `intentId` proti katalogu nebo bezpečného podmnoží.

## Důsledky

- Až bude LLM špatně generovat / halucinovat, rule engine zůstává deterministickou zálohou.
- Velikost APK a údržba NDK/MediaPipe sady jsou oddělené od „logické“ skladby NLU (lze testovat chain samostatně).
- Bez fáze 2 se **nepočítá** s nižší latencí ani jinými vlastnostmi LLM — úkol `[F5-T01]` zůstává otevřený do hotové inference.

## Související

- [`0002-ecapa-speaker-onnx.md`](./0002-ecapa-speaker-onnx.md) — precedens pro binárky mimo git.
- [`0007-anti-spoofing-onnx.md`](./0007-anti-spoofing-onnx.md) — další ONNX/váhy ve F5.
