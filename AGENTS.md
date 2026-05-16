# AGENTS.md — pravidla pro agentní workery v projektu Handy

Tento dokument je závazný pro každého AI agenta nebo lidského přispěvatele, který pracuje na úkolech v tomto repozitáři.

## Zdroj pravdy

- **Plán a fáze:** `IMPLEMENTATION_PLAN.md`
- **Vizuální dashboard:** `progress.html` (uživatel ho otevírá v prohlížeči)
- **Architektonická rozhodnutí:** `docs/decisions/NNNN-*.md` (ADR formát)

## Workflow pro jeden úkol

1. **Vyber úkol** s nejnižším volným ID v aktuální fázi (`status: pending`). Respektuj pořadí fází: nepouštěj se do F2, dokud F1 není `done` nebo aspoň všechny závislosti hotové.
2. **Označ ho jako rozpracovaný** — v `progress.html` nastav `"status": "in_progress"` pro daný `id`. Aktualizuj `"lastUpdated"`. Maximálně jeden úkol per worker v `in_progress`.
3. **Implementuj** podle popisu úkolu. Postupuj TDD kdekoli to dává smysl. Při větším úkolu rozděl práci na menší commity, ale **status zůstává `in_progress`** dokud není hotový celý úkol.
4. **Ověř Definition of Done** dané fáze (viz `IMPLEMENTATION_PLAN.md`). Pokud úkol má vlastní akceptační kritérium v popisu, splň ho explicitně a doloží to (čísla, screenshot, log).
5. **Označ jako hotový:**
   - V `IMPLEMENTATION_PLAN.md` změň `- [ ]` na `- [x]` u daného `[ID]`.
   - V `IMPLEMENTATION_PLAN.md` přidej řádek do Changelog: `YYYY-MM-DD  [ID]  Krátký popis  (worker: <handle>)`.
   - V `progress.html` nastav `"status": "done"`, `"completedAt": "YYYY-MM-DD"`. Pokud byly k úkolu poznámky, zachovej je v `"notes"`.
   - V `progress.html` aktualizuj `"lastUpdated"`.
6. **Commit:** jeden úkol = jeden Conventional Commits commit, např. `feat(F1-T03): integrate ECAPA-TDNN speaker verification`. Pokud commit obsahuje více úkolů, je to chyba — split.

## Když je úkol zablokovaný

- V `progress.html`: nastav `"status": "blocked"`, do `"notes"` napiš důvod ("čeká na D-003 rozhodnutí", "BT zařízení nedostupné", apod.).
- Nestrkej blocked úkol do Changelog.
- Vyber jiný úkol, který blokovaný úkol nepředpokládá.

## Když chceš přidat nový úkol

- **Neudělej to sám.** Otevři diskuzi (issue / DM uživateli) a vysvětli, proč. Po schválení:
  1. Přiděl nové ID podle konvence `F<phase>-T<dvouciferné_pořadí>`.
  2. Přidej řádek do `IMPLEMENTATION_PLAN.md` ve správné fázi (zachovej numerické pořadí).
  3. Přidej objekt do `progress.html` ve stejné fázi se stejným `id`, výchozí `status: "pending"`.

## Pravidla kvality

- **Kotlin:** ktlint + detekt, žádné warningy.
- **Komentáře:** vysvětluj *proč*, ne *co*. Žádné triviální komentáře k samoZjevnému kódu.
- **Testy:** každý nový intent v `:feature:nlu` má pattern unit testy. Každý nový adaptér v `:feature:actions` má integration test (nebo zdůvodněnou výjimku).
- **Žádné cloud API.** Pokud chceš použít knihovnu, která dělá síťové volání pro core funkcionalitu, **stop** a zeptej se uživatele.
- **Žádné nahrávání audia mimo zařízení.** Ani v telemetrii, ani v crash reportech.
- **ADR:** každé technologické rozhodnutí, které nelze rozumně vrátit zpět během sprintu (volba ASR enginu, NLU engine, distribuční kanál), vyžaduje ADR v `docs/decisions/`.

## Konvence Git

- Branch: `f<phase>/t<task>-<kebab-summary>`, např. `f1/t03-ecapa-storage`.
- Commit message: Conventional Commits s ID v scope: `feat(F1-T03): …`, `fix(F2-T11): …`, `chore(F0-T03): …`.
- PR title: `[F1-T03] integrate ECAPA-TDNN speaker verification`.
- PR body povinně obsahuje:
  - odkaz na ID v plánu,
  - jak byla splněna Definition of Done,
  - důkaz (čísla, screenshot, video, log).

## Když opustíš úkol uprostřed

- Vrať `status` na `"pending"` (nebo `"blocked"` s důvodem).
- V Changelog nepřidávej řádek (úkol není done).
- Commit-uj rozdělanou práci s prefixem `wip(F1-T03):` a označ branch jako draft.

## Závěr

Tabulka v `progress.html` je to, co se ukazuje uživateli. **Když ti ukáže prst na buňku a řekne "to není pravda", je to tvůj problém.** Mark complete znamená *prokazatelně funkční, otestované, dokumentované*.
