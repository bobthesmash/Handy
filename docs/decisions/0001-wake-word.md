# ADR 0001: Výběr wake-word enginu (Porcupine × openWakeWord / Re-MENTIA)

- **Status:** accepted (F0-T05 PoC)
- **Datum:** 2026-05-15
- **Kontext:** Handy vyžaduje nízkou latenci, běh na zařízení a **žádný odesílaný hlas** mimo phone. Potřebujeme dva pohledy: komerční proprietární řešení s podporou vs. open-source ONNX stack.

## Možnosti

| Kritér | Picovoice **Porcupine** | **openWakeWord** přes Re-MENTIA (`xyz.rementia:openwakeword`) |
|--------|--------------------------|---------------------------------------------------------------|
| **Licence** | Proprietární SDK zdarma osobním vývojářům od Picovoice; custom keyword přes Konzoli | Apache-2.0 (binding + ONNX modely převážně Apache-2.0; embedding dle upstream) |
| **AccessKey / účet** | Vyžaduje bezplatnou registraci Picovoice Console (`AccessKey`, nesdílet veřejně) | Účet nepotřebný pro inference; ONNX soubory lze hostovat jen v APK |
| **Síť** | Dokumentovaná `INTERNET` permission (**validace přístupového klíče / licensing**). Inference běží na zařízení; **audio se neuploaduje jako součást engine API**. | Inference zcela offline po nasazení modelů (`onnxruntime-android`) |
| **Integrace** | `ai.picovoice:porcupine-android` — low-level `Porcupine.process(ShortArray)` ve frekvenci a délce rámců dané SDK | `WakeWordEngine` + vlastní ONNX (mel, embedding, classifier) pod `assets/openwakeword/` |
| **Údržba mikrofonu** | Lze použít jen low-level API s vlastním `AudioRecord`/ring bufferem (Handy `:core:audio`). | Veřejné API `WakeWordEngine.start()` váže **vlastní** `AudioRecord` v knihovně (`AudioRecorder`, 16 kHz mono); vstupní PCM zvenku **není** exportované (`AudioProcessor` je `internal`). Paralelní recorder vedle `EarService` je možný jen jako experiment — viz Důsledky. |
| **Latence / RAM** | Marketingově velmi nízká stopa; profilovat na konkrétním chipsetu (`F0-T07`). | Těžší kvůli ONNX runtime + pipeline; měření po dodání přesných vah |

## Rozhodnutí

1. **Do repozitáře vstupují obě závislosti** pro srovnávací linku F0/F1:
   - **Porcupine** — výchozí produktová cesta dle projektového plánu (`IMPLEMENTATION_PLAN.md`).
   - **openWakeWord (Re-MENTIA)** — referenční open pipeline; **neběží automaticky bez ONNX souborů** v `feature:wakeword/src/main/assets/openwakeword/`.

2. **Debug micro-benchmark (F0-T05):** po startu aplikace (`BuildConfig.DEBUG`) `WakeWordEnginesProbe` měří jen **Porcupine `process` na syntetické ticho** (`:feature:wakeword`). Bez `picovoice.access.key` benchmark přeskočí (bez failu CI).

3. **INTERNET oprávnění** (`:app`): přidáno výhradně kvůli **Picovoice SDK** dokumentaci „AccessKey validation“. **Handy nesmí odesílat audio na cloud.** Pokud Pozdější audit odhalí nežádoucí telemetrii, řeší se novým ADR případnou náhradu enginu.

4. **Konfigurace klíče:** `local.properties`:
   ```
   picovoice.access.key=VAŠE_PICVOICE_CONSOLE_KEY
   ```
   Alternativa: `-PICVOICE_ACCESS_KEY=...` v Gradle invocation. **Nikdy necommitovat klíče.**

## Důsledky

- Produční wake-word v aplikaci běží jako **`PorcupineEarWakePump`**: jeden zdroj PCM přes ring buffer ze **`EarService`** (`EarAudioBridge`), bez druhého `AudioRecord`.
- **`WakeWordEngine` (Re-MENTIA)** ve verzi závislosti projektu (`xyz.rementia:openwakeword`) je uzavřený na vlastní nahrávání; sdílený buffer se stejným vzorkováním jako `EarService` by vyžadoval **upstream změnu** (veřejný feed PCM / znovupoužitelný processor) nebo **fork**. Do té doby nepouštět oba enginy současně na mikrofonu, pokud OEM/Android neumožňuje bezpečný paralelní přístup — preferovat jen Porcupine v hlavní smyčce.
- Vývojář bez klíče a bez ONNX výrazů stále **buildí** projekt (openWakeWord se „odmlčí“, Porcupine se přeskočí).
- Pro plné end-to-end srovnání je potřeba:
  - nasadit ONNX dle README v assetech,
  - po vyřešení práv použít vlastní klasifikátor pojmenovaný v kódu (`OpenWakeWordEngineFactory.FILE_KEYWORD`),
  - dodržovat pravidlo **žádné nahrávky mimo zařízení** v telemetrii a logování wake-word pipeline.

## Odkazy

- Picovoice Android QS: https://picovoice.ai/docs/quick-start/porcupine-android/
- Maven `porcupine-android`: https://repo1.maven.org/maven2/ai/picovoice/porcupine-android/
- Re-MENTIA binding: https://github.com/Re-MENTIA/openwakeword-android-kt
- Projektové pravidla: root `AGENTS.md`, `IMPLEMENTATION_PLAN.md` (`[F0-T05]`)
