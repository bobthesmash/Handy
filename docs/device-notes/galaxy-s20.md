# Samsung Galaxy S20 — referenční materiál (`[D-001]`)

Stručné technické fakta související s řadou **Galaxy S20 / S20+ / S20 Ultra** (v „O telefonu“ typicky kódy **SM-G98x** podle regionu a submodelu; Exynos vs Snapdragon podle SKU). Slouží jako kontext u měření v `docs/benchmarks/` a u `docs/qa/lockscreen-matrix.md`; není závazný výčet všech použitelných telefonů.

## Hardware / řada

- Jméno řady v uživatelském rozhraní „Galaxy S20“; přesný kód modelu (**Nastavení → O telefonu**) ovlivní SoC variantu a někdy chování DSP.
- Stereo reproduktor + vestavěné mikfony; bluetooth SCO používá jiný audio graph než vestavěný mikrofon (viz ADR audio / `EarService`).

## One UI / Samsung vrstva

- **Spoření baterie na aplikaci** může ovlivnit CPU wake v pozadí a notifikaci FGS; porovnávání měření baterie mezi zapnutým / vypnutým omezením dává jiná čísla (to je vlastnost OEM, ne chyba Handy).
- **Uzamčený keyguard**: Samsung často mění pravidla pro oznámení a vynucené oprávnění (přístup k oznámením, aktivita přes lockscreen) — hodí se evidovat vedle výsledků v QA matrix.

## Wake / konkurence hlasových funkcí

- Systém může mít vlastní aktivaci hlasové asistence (závislé na verzi UI a regionu); souběžné chování s Porcupine nelze ze zdrojového kódu Handy deterministicky slíbit — zapisovat se k výsledkům měření, ne interpretovat jako regressi bez OEM kontextu.

## Odkazy v tomto repu

- Obecnější odkazy k výrobcům: obrazovka `OemManufacturerHintsScreen` / dontkillmyapp (Samsung je v seznamu).
- Latence / logcat: `docs/benchmarks/latency.md`
- Battery Historian protokol: `docs/benchmarks/battery.md`
