# Wake → pipeline latence (`F0-T07`)

## Stav měření

**V tomto repozitáři nejsou doplněná ověřená čísla** — tabulka „Záznam měření“ níže zůstává prázdná, dokud neproběhne měření na fyzickém zařízení. Postup a logové tagy jsou závazný **protokol**, ne výsledek. Úkol `[F0-T07]` je v `progress.html` veden jako **`blocked`** do doby HW.

### Referenční HW (`[D-001]`)

**Samsung Galaxy S20** (regionálně SM-G980x / Exynos nebo Snapdragon podle SKU). Při prvním měření dopiš do tabulky níže přesný **Android / API level**, **One UI** (pokud je uvedeno v „O telefonu“) a **build číslo**.

Další poznámky k One UI / baterii / keyguardu: [`docs/device-notes/galaxy-s20.md`](../device-notes/galaxy-s20.md).

Šablona měření pro referenční telefon.

## Wake-word → ASR-ready (cíl F0 Definition of Done: ≤ 700 ms)

| Kroky | Poznámka |
|-------|---------|
| 1 | Spusť `Handy`, připoj BT handsfree headset. |
| 2 | V logcat vyfiltruj tag `HandyWwBench` po cold start — zapiš `avgProcessMs` pro Porcupine (bez ASR je to jen rámcové inference). |
| 3 | Pro **Sherpa graf**: na domovské obrazovce (demo panel s MVP řetězcem) klepni **„Přednačíst ASR (simulace wake…“** — volá [HandyAssistantViewModel.noteWakeWordForHeavyModels]. V logcat filtruj tag **`HandyLatency`**: řádek `sherpa_ready wakeToReadyMs=…` je čas od signálu wake po dokončení [SherpaStreamingRecognizerHolder.acquire] (příprava ONNX grafa; ještě **ne** první partial z mikrofonu). |
| 4 | Až bude mikro-buffer krmit Sherpu, přibude řádek `first_partial wakeToPartialMs=… nonEmpty=…` — ten porovnej s cílem ≤ 700 ms E2E. |

### Logcat (ukázka)

```text
adb logcat -s HandyLatency HandyWwBench
```

### Záznam měření (doplňovat při testech)

| Datum | Zařízení / API | Wake engine | Wake→Sherpa ready (ms) | Wake→1. partial (ms) | Wake→Porcupine frame (avg ms) |
|-------|----------------|-------------|------------------------|------------------------|------------------------------|
| _ | _ | _ | _ | _ | _ |

## Poznámky

- Aktuální build loguje jen **Porcupine `process`** průměr (viz `WakeWordEnginesProbe`) — řádek „avgProcessMs“ je spodní odhad čisté náklady inference, **ne** E2E latence.
- **`HandyLatency`** neobsahuje text ASR — jen čísla a `nonEmpty` u prvního partial (až bude napojeno).
- Po `noteWakeWordForHeavyModels` běží smyčka **EarService → [MonoPcmRingBuffer] → Sherpa**; `first_partial` se vypíše při **prvním neprázdném** partial textu.

