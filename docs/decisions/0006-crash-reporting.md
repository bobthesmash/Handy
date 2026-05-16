# ADR 0006: Crash reporting a ochrana citlivých dat

## Stav

- **Rozhodnutí:** Handy zůstává **bez povinného cloud crash reporteru**. Výchozí strategie je **lokální pozorovatel** (volitelný, níže) + standardní kanály (Play/Android Vitals po publikaci v obchodě, `adb logcat` / bugreport při vývoji).
- **Volitelná budoucí extenze:** **opt‑in** integrace třetí strany (např. Sentry) **jen** po výslovném souhlasu v nastavení, s tvrdými pravidly redakce.

## Kontext

- Projekt nese **biometrický profil** (ECAPA embeddingy), **aliasy kontaktů** a historii příkazů — nesmí se dostat do přiložených dat pádu.
- `AGENTS.md`: **žádné nahrávání audia** mimo zařízení — platí i pro crash reporty a breadcrumby.
- Síťová knihovna jen pro **vedlejší** diagnostiku (ne pro jádro ASR/NLU) je přijatelná po etickém a právním posouzení a výslovné volbě uživatele (`AGENTS.md` — cloud jen mimo „core“ pipeline).

## Zvažované varianty

| Varianta | Plusy | Mínusy |
|--------|--------|--------|
| A — Pouze Play Vitals / ruční bugreport | Žádný extra kód, žádný DSN | Méně kontextu při sideload beta |
| B — Lokální soubor + sdílení (SAF) | Uživatel drží data, vhodné pro asistovaný support | Vyžaduje UI a disciplínu při redakci |
| C — Opt‑in Sentry (nebo analog) | Sklony, symbolikace, alerting | DSGVO, subprocessory, riziko přetečení PII při špatné konfiguraci |

## Rozhodnutí (detail)

1. **Žádný Firebase Crashlytics / automatický Sentry v default buildu** bez uživatelského přepínače.
2. **Minimální telemetrie pádu může být jen lokální:** zápis stack trace + verze buildu do `filesDir` (např. posledních N záznamů), **bez** automatického uploadu.
3. Když v budoucnu přibude **opt‑in Sentry** (nebo jiný poskytovatel):
   - výchozí **vypnuto**;
   - v politice soukromí zmínka podprocessorů a účelu;
   - **zakázané** přílohy audio, raw embeddingy, celé notification texty a volné texty uživatele v breadcrumbách;
   - preferovat **release build + ProGuard mapping** jen v interním CI, ne sdílet mapping veřejně bez kontroly.

## Důsledky

- Úkoly typu „zapojit Sentry“ patří až **po** explicitní product/legal kontrole a UI tématu „Diagnostika → Sdílet pády (volitelně)“.
- QA a beta spoléhají na **lockscreen matici** + **lokální NDJSON telemetrii** (`F2-T13`) pro frekvence, nikoli na cloud stack traces.

## Odkazy

- Android Vitals: [https://developer.android.com/topic/performance/vitals](https://developer.android.com/topic/performance/vitals)
