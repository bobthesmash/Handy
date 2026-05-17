# ADR-0009: Výchozí produktový směr v1 (jazyk, wake word, distribuce, TTS, anti-spoof, beta účast)

## Stav

Accepted (2026-05-17)

## Kontext

Položky `[D-002]`–`[D-007]` v `IMPLEMENTATION_PLAN.md` blokovaly „uzavření“ plánu bez zápisu směru. Vlastník projektu požadoval dokončit vše kromě měření a QA na fyzickém HW (`F0-T07`, `F0-T08`, `F1-T22`, `F1-T23`, `F3-T08`). Tento ADR fixuje **aktuální výchozí volby v1**, konzistentní s existujícím kódem a s předchozími ADR; měření na Samsung Galaxy S20 (`[D-001]`) může vést k úpravám prioritzací, ne k rozporu s offline/cloud pravidly z `AGENTS.md`.

## Rozhodnutí

| ID | Rozhodnutí |
|----|------------|
| **D-002** | **Primárně čeština (cs-CZ)** pro v1. Angličtina a automatické přepínání jazyka jsou v backlogu `[F5-T03]` a vyžadují samostatné ASR/NLU assety a QA — mimo současný scope. |
| **D-003** | **Porcupine** jako výchozí produkční wake word podle [ADR-0001](./0001-wake-word.md). **openWakeWord** zůstává experimentální / benchmark větev ve stejném ADR; finální srovnání na referenčním HW doplňuje tabulky v `docs/benchmarks/`, ale nevylučuje shipping Porcupine jako default v1. |
| **D-004** | **Primární distribuce: sideload** (`adb install -r`, viz `README.md`). **Google Play** jako následný cíl po beta a po dokončení sensitive permission review. **F-Droid** explicitně `[F5-T07]`. |
| **D-005** | **Android Speech Services / Google český TTS** jako výchozí implementace v1 (`AndroidCzechSpeechSynthesizer`). **Piper** plně offline CZ až `[F5-T04]` (větší APK, samostatná integrace). |
| **D-006** | **Anti-spoofing až F5** (`[F5-T02]`, [ADR-0007](./0007-anti-spoofing-onnx.md)). MVP spoléhá na ECAPA + dual threshold + confirm gate; ONNX `anti_spoof.onnx` zůstává volitelný dodatek. |
| **D-007** | **Zapojení cílového uživatele od fáze F4 (beta)**, nikoli jako povinná brzda pro PoC měření F0 ani vývojové milestone F1–F3. Alfa/PoC mohou proběhnout bez něj; strukturovaný beta feedback je `[F4-T06]`. |

## Důsledky

- Žádná změna kódu není z tohoto ADR povinná — dokumentuje již převažující stav repozitáře.
- Úkoly `[F4-T06]` / `[F4-T07]` zůstávají závislé na **reálném běhu s lidmi** (nelze označit `done` bez activity vlastníka).
- Při změně směru (např. Play-first, EN paralelně) přidej nový ADR nebo revizi tohoto dokumentu místo mlčky rozjezdů v kódu.

## Související

- `IMPLEMENTATION_PLAN.md` — sekce rozhodnutí `D-002` … `D-007`
- `progress.html` — řádky `D-002` … `D-007` (`done` + odkaz sem)
