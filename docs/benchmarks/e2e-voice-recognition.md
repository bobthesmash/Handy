# E2E: 50 nahrávek vlastního hlasu — úspěšnost rozpoznání ([F1-T22])

## Cíl

≥ 90 % frází musí projít řetězcem **ASR → NLU → správný intent + sloty** (nebo ekvivalentní metrika projektu).

## Příprava

1. Jedno cílové zařízení — referenční výběr projektu **[D-001]: Samsung Galaxy S20** (jiný telefon jen pokud explicitně dokumentuješ odchylku); přesný Android / build zapisovat ve výsledku níže.
2. Nahrané nebo živě generované **50 krátkých českých vět** pokrývajících MVP intenty (`HandyNluCatalogs.mvp`).
3. Zapnutý speaker-verify dle ostrého nastavení (`T_high` / `T_low`).

Kontext k referenčnímu Samsungu / One UI: [`../device-notes/galaxy-s20.md`](../device-notes/galaxy-s20.md).

## Protokol

| # | Přepis (ground truth) | Očekávaný intent | Poznámka / pass–fail |
|---|------------------------|-------------------|----------------------|
| 1 | … | … | |
| … | … | … | |
| 50 | … | … | |

- **Pass:** přepis ASR normalizací NLU nevadí; `intentId` a klíčové sloty odpovídají šabloně.
- **Fail:** špatný intent, chybějící slot, nebo selhání verify.

## Výsledek

- **Pass rate:** ___ / 50 = \_\_\_ %  
- **Datum / verze buildu / worker:**  
- **Problémové fráze (log / soubor):**

Po vyplnění tabulky a dosažení ≥ 90 % lze v plánu označit **[F1-T22]** jako hotové s odkazem na tento soubor (nebo kopii v repu bez audia).
