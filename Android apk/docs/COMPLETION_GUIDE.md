# Handy — Návod jak dokončit projekt

> **PRO LLM WORKERY:** Tohle čti **jako první** než cokoli uděláš. Tenhle soubor je
> orientační mapa pro dokončení projektu Handy a má **přednost** před jakoukoli
> kreativitou. Pokud něco není v `IMPLEMENTATION_PLAN.md`, `progress.html` nebo
> tady — **neexistuje to**, nesmíš to vymyslet.
>
> Tento dokument je deskriptivní (popisuje co zbývá a v jakém pořadí); zdroj
> pravdy zůstává `IMPLEMENTATION_PLAN.md` + `progress.html` + `AGENTS.md`.

---

## 0. Než se na cokoli sáhneš — povinný start checklist

1. Přečti `AGENTS.md` (workflow per úkol).
2. Otevři `progress.html` v prohlížeči nebo v editoru a podívej se na řádky se
   statusem `pending` / `in_progress` / `blocked`. Pořadí: F0 → F1 → F2 → F3
   → F4 → F5 → D.
3. Pokud je úkol `blocked`, **nepracuj na něm**. Důvod blokace je v `notes`
   pole tasku — typicky čeká na fyzický telefon (Samsung Galaxy S20, viz
   `[D-001]`) nebo na rozhodnutí uživatele.
4. Vyber úkol s nejnižším volným ID v aktuální fázi se statusem `pending` —
   ne `blocked`, ne `done`, ne `in_progress` (ten už dělá někdo jiný).
5. Označ ho jako `in_progress` v `progress.html`, jeden worker = jeden
   úkol `in_progress`.

Pokud nejsi schopen tohle dodržet — **přestaň pracovat a zeptej se uživatele**.

---

## 1. Aktuální stav projektu (k datu vzniku tohoto dokumentu)

Statistika z `progress.html` (mimo F5 a D):

| Fáze | Done | Pending | Blocked | Total | Kdo blokuje |
|------|------|---------|---------|-------|-------------|
| F0 — Foundation & PoC | 8 | 0 | 2 | 10 | HW (S20) |
| F1 — MVP Core | 21 | 0 | 2 | 23 | HW (nahrávky vzorků) |
| F2 — Rozšířené příkazy | 13 | 0 | 0 | 13 | — hotovo |
| F3 — Lockscreen & energy | 7 | 0 | 1 | 8 | HW (field test) |
| F4 — Polish & beta | 5 | 2 | 0 | 7 | Beta testeři |
| D — Otevřená rozhodnutí | 1 | 6 | 0 | 7 | Uživatel |

Procentuálně **F0–F4 ≈ 87 %** hotových úkolů. Zbývá **7 reálných úkolů** + 6
otevřených rozhodnutí. **Nic z toho nelze podvrhnout vymyšleným kódem.**

---

## 2. Co konkrétně zbývá (jednoznačně)

### 2.A — Vyžaduje fyzické zařízení Samsung Galaxy S20 (uživatelská relace)

> Pro tyhle úkoly **NIKDY** neoznačuj `done`, dokud v příslušném souboru
> v `docs/benchmarks/` nebo `docs/qa/` **fyzicky nejsou řádky s naměřenými
> čísly nebo OK/FAIL buňky**. Šablona ≠ splnění. Záhlaví těchto souborů to
> říká explicitně, nečtěte je selektivně.

| ID | Co | Soubor pro zápis výsledku | DoD |
|----|-----|---------------------------|-----|
| `F0-T07` | Latence wake → ASR-ready | `docs/benchmarks/latency.md` | ≤ 700 ms |
| `F0-T08` | Spotřeba baterie idle/listening | `docs/benchmarks/battery.md` | ≤ 6 %/h |
| `F1-T22` | 50 nahrávek vlastního hlasu | `docs/benchmarks/e2e-voice-recognition.md` | ≥ 90 % recognition |
| `F1-T23` | 30 nahrávek cizích hlasů | `docs/benchmarks/false-accept-voice.md` | < 2 % false accept |
| `F3-T08` | Field test 1 den v kapse + BT | `docs/qa/lockscreen-matrix.md` poznámky | 80 % v poli |
| `F4-T06` | Beta 3–5 reálných uživatelů | feedback v aplikaci → export | feedback od ≥ 3 lidí |
| `F4-T07` | Iterace + release candidate | tag/branch, commit | RC build instalovatelný |

### 2.B — Otevřená rozhodnutí uživatele (`D-002` až `D-007`)

Tyhle nesmíš zodpovědět sám. Žádáš uživatele a po odpovědi:
- Zaznamenat **buď** v existujícím ADR `docs/decisions/000N-*.md`,
- **nebo** přidat nový ADR podle vzoru (pravidlo z `AGENTS.md`).
- Status v `progress.html` přepnout na `done` s `completedAt` = den
  rozhodnutí.

| ID | Otázka | Bez čeho neblokuje | Co blokuje |
|----|--------|--------------------|------------|
| `D-002` | CZ-only nebo CZ+EN? | F0–F4 (CZ default) | F5-T03 multi-jazyk |
| `D-003` | Porcupine vs openWakeWord | `F0-T05` ADR-0001 doporučuje Porcupine | finální výběr v release |
| `D-004` | Play Store vs sideload/F-Droid | dev build | distribuci, sensitive permission review |
| `D-005` | Google TTS CZ vs Piper offline | F1 (default Android TTS) | F5-T04 |
| `D-006` | Anti-spoofing v MVP nebo F5? | F1 (zatím F5) | priorita F5-T02 |
| `D-007` | Účast cílového uživatele od které fáze? | nic, default „beta" | tempo alfa testů |

### 2.C — Volitelné práce z `audit-remediation-checklist.html`

Tyhle můžeš dělat **jen mezi blokovanými úkoly** a **jen pokud sedí do scope
aktuální fáze**. Nepřidávej je do `IMPLEMENTATION_PLAN.md` jako nové tasky.

| ID | Co | Připomínka |
|----|-----|------------|
| `H-01` | Golden-vector test ECAPA preprocessoru | Vyžaduje předem připravený SpeechBrain referenční embedding (fixture). |
| `H-02` | FFT v preprocessoru O(N²) → O(N log N) | Confirm gate latence; mít benchmark před/po. |
| `H-03` | Zpřísnit `config/detekt/detekt.yml` | LongMethod, Cyclomatic. |
| `H-04` | JaCoCo coverage i mimo `:feature:nlu` | Jen tam, kde dává smysl. |
| `I-02` | README CI badge owner/repo | Owner už je `bobthesmash`, zkontroluj že nikde nezbyl placeholder. |
| `E-03` | Smoke `./gradlew :app:assembleRelease` | Není automatizováno v `ciHandy`. |

---

## 3. Co je dokončené a NESMÍŠ to znovu otevírat

**Hotové fáze / oblasti** (revize jen pokud uživatel explicitně řekne):

- Celá **F2** (intent palette: REPLY_NOTIF, PLAY_MEDIA, MEDIA_CTRL, OPEN_APP,
  NAVIGATE, WHAT_TIME/DATE/BATTERY, TIMER, CANCEL/STOP/REPEAT, AGC,
  NoiseSuppressor/AEC, ASR confidence, lokální off-by-default telemetrie).
- **NLU + DSL + RuleBasedNluEngine** (`F1-T08`), katalog `HandyNluCatalogs.mvp`.
- **Speaker pipeline** (`F1-T02`/`T03`/`T04` + `F1-T16` confirm gate, ECAPA
  + DualThreshold + EncryptedSharedPreferences).
- **VAD + ASR** (Silero + Sherpa-onnx + AAR `com.xdcobra.sherpa:sherpa-onnx`
  verze v `gradle/libs.versions.toml`).
- **EarService FGS** + `MonoPcmRingBuffer` 3 s + BT SCO routing + boot
  receiver.
- **Wake-word Porcupine runtime** + openWakeWord experiment (ADR-0001).
- **Persistence**: Room `HandyDatabase` v1, `:core:persistence`.
- **Privacy policy v1.2 CZ+EN**, Play Data safety podklad, `PrivacyPolicyScreen`.
- **Backup/restore profilu** (`HandyProfileBackup` AES-GCM + PBKDF2).
- **OEM hints screen**, OnboardingPermissions wizard, **TTS modul** (Android cs-CZ).
- **Crash reporting ADR** (`0006-crash-reporting.md`, default bez cloudu).

> Kdyby ti kterýkoli z těchto bodů přišel rozpracovaný, **pravděpodobně se mýlíš
> ty**, ne projekt. Otevři příslušný changelog řádek v `IMPLEMENTATION_PLAN.md`,
> najdi commit, ověř kód. Pokud opravdu chybí, ohlas to uživateli a nepřepisuj
> historii.

---

## 4. Závazný postup pro každý dokončený úkol

Pochází z `AGENTS.md`. Tady jen kompaktně:

1. Branch `f<phase>/t<task>-<kebab-summary>` (např. `f4/t06-beta-rollout`).
2. Implementuj (TDD kde dává smysl).
3. **Ověř Definition of Done dané fáze** + případně akceptační kritérium
   v `notes` tasku.
4. V `IMPLEMENTATION_PLAN.md`: `- [ ]` → `- [x]` u příslušného `[ID]`.
5. V `IMPLEMENTATION_PLAN.md`: jeden řádek do **Changelog** ve formátu
   `YYYY-MM-DD  [ID]  Krátký popis  (worker: <handle>)`.
6. V `progress.html`: tasku stejné ID nastav `"status": "done"`,
   `"completedAt": "YYYY-MM-DD"`, případně poznámku do `"notes"`. Aktualizuj
   `"lastUpdated"` v rootu.
7. Conventional Commits: jeden úkol = jeden commit
   (`feat(F1-T22): 50-sample own voice recognition pass rate 92 %`).

Když úkol opustíš nehotový: `status` zpět na `pending` (nebo `blocked` s
důvodem v `notes`), žádný Changelog řádek, branch zůstává jako draft, commit
prefix `wip(F?-T??):`.

---

## 5. Absolutní zákazy (časté halucinace nízkokontextových botů)

1. **Nikdy** nepřidávej cloud API call do core funkcionality (ASR, NLU,
   biometrika, telemetrie). Pravidlo z `AGENTS.md`.
2. **Nikdy** neukládej audio mimo zařízení — ani do crash reportů, ani do
   telemetrie. Privacy policy v1.2 to deklaruje navenek.
3. **Nikdy** neoznačuj HW-blocked úkol (`F0-T07`, `F0-T08`, `F1-T22`,
   `F1-T23`, `F3-T08`) jako `done` na základě existence šablony,
   commentáře, nebo testu bez reálných čísel.
4. **Nikdy** nevytvářej nový task ID. Když opravdu potřebuješ, zeptej se
   uživatele (`AGENTS.md` → sekce „Když chceš přidat nový úkol").
5. **Nikdy** neměň strukturu `progress.html` mimo JSON blok (HTML / CSS /
   JS je zamčené).
6. **Nikdy** neměň hotové úkoly z `done` zpátky na `pending` bez explicitní
   shody s uživatelem (chyba v changelogu = audit problém).
7. **Nikdy** nepoužij více než jeden `in_progress` na worker (rule z
   `AGENTS.md`).
8. **Nikdy** nedoplňuj měření do `docs/benchmarks/` ze "syntetických"
   výsledků nebo z odhadu. Pokud nemáš HW, nech `blocked`.

---

## 6. Definitivní DONE pro celý projekt (release candidate)

Projekt je **„hotový"** (RC připravený k beta distribuci) když platí
**všechno** z následujícího:

1. **Všechny úkoly v F0–F4** v `progress.html` mají status `done`
   (žádné `pending`, žádné `blocked`).
2. **DoD F0:** Latence ≤ 700 ms a baterie ≤ 6 %/h **prokázány řádky** v
   `docs/benchmarks/latency.md` a `docs/benchmarks/battery.md`.
3. **DoD F1:** ≥ 90 % own recognition (řádky v
   `docs/benchmarks/e2e-voice-recognition.md`) a < 2 % false-accept
   (řádky v `docs/benchmarks/false-accept-voice.md`).
4. **DoD F2:** Splněno — všechny F2 intenty zelené v unit testech a NLU
   katalogu.
5. **DoD F3:** Matrix v `docs/qa/lockscreen-matrix.md` má **OK/FAIL** v
   každé buňce u F1+F2 intentů; baterie ≤ 5 %/h v 8h scénáři.
6. **DoD F4:** Privacy policy hotová (✓ je); beta s ≥ 3 testery
   dokončená; RC tag/branch v repu.
7. **Otevřená rozhodnutí D-002..D-007:** každé minimálně zaznamenané v
   ADR v `docs/decisions/` nebo má status `done` v `progress.html`.
8. **CI badge zelený** v
   `https://github.com/bobthesmash/Handy/actions/workflows/ci.yml`.
9. **`./gradlew :app:assembleRelease`** projde lokálně i v CI; APK je
   instalovatelný přes `adb install -r` (sekce „Side-load" v `README.md`).

> Tabulka v `progress.html` je to, co uživatel vidí. Stav `done` musí znamenat
> **prokazatelně funkční, otestované, dokumentované** (citace z `AGENTS.md`).

---

## 7. Pořadí dokončování (doporučený sled)

Pokud má uživatel hodinu volného času, prioritizuj v tomto pořadí:

### Krátký sprint (≤ 1 h s telefonem v ruce)
1. **`F0-T07`** + **`F0-T08`** současně — jedna session v aplikaci, zápis čísel
   do `docs/benchmarks/latency.md` a `battery.md`. **Odblokuje DoD F0.**

### Střední sprint (1 den)
2. **`F1-T22`** — 50 frází z `HandyNluCatalogs.mvp` od uživatele.
3. **`F1-T23`** — 30 cizích vzorků (rodinní příslušníci, kolegové).
   **Odblokuje DoD F1.**
4. **`F3-T08`** — celodenní field test (telefon v kapse + BT, deník selhání
   do poznámek). **Odblokuje DoD F3.**

### Dlouhý sprint (1–2 týdny)
5. **Rozhodnutí D-002..D-007** — krátký rozhovor s uživatelem + 5–7 ADR.
6. **`F4-T06`** — najít 3–5 beta testerů, sdílet APK, sebrat feedback.
7. **`F4-T07`** — iterace bugfixů, vytvořit RC.

### Mezi tím (AI agent samostatně)
- `H-01`..`H-04` z audit checklistu (kvalita kódu).
- `I-02` (CI badge — pravděpodobně už hotové, jen verify).
- `E-03` (release build smoke).

### Po RC (F5, jen na požadavek)
- F5-T01..T07 — nice-to-have, **bez timeline**, řešit až po beta.

---

## 8. Když nevíš co dělat

Místo halucinace:

1. **Spusť `./gradlew ciHandy`** — pokud červené, oprav.
2. **Zkontroluj drift** mezi `IMPLEMENTATION_PLAN.md` a `progress.html` —
   pokud někde stav nesouhlasí, je to bug.
3. **Otevři `audit-remediation-checklist.html`** — najdi položku bez
   `doneNote` a implementuj ji (jen ty, které nejsou označené jako
   už hotové).
4. **Přečti changelog v `IMPLEMENTATION_PLAN.md`** — ujisti se, že rozumíš
   poslednímu commitu před vlastní prací.
5. **Zeptej se uživatele.** Konkrétní otázka s rozhodovací volbou je vždy
   lepší než vymyšlený kód.

---

## 9. Cheat-sheet jednou větou

> Otevři `progress.html`, najdi nejnižší `pending` v aktuální fázi (ne
> `blocked`!), nastav `in_progress`, splň DoD fáze, ulož do
> `IMPLEMENTATION_PLAN.md` (`[x]` + Changelog), přepiš `progress.html`
> (`done` + `completedAt` + `lastUpdated`), jeden Conventional Commit per ID.
> Tečka.

---

**Datum vzniku tohoto návodu:** 2026-05-16. Pokud se neshoduje s aktuálním
stavem `progress.html`, řiď se `progress.html` — ten je zdrojem pravdy.
