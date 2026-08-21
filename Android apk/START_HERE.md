# Handy — Android Studio (sync → build)

## 3 kroky

1. **File → Open** → složka **`D:\Handy`** (kořen s `settings.gradle.kts`, ne tato podsložka `Android apk`).
2. **Sync Project with Gradle Files** — při prvním otevření se vytvoří `local.properties` se `sdk.dir`, pokud máš standardní Android SDK.
3. **Run** — v horní liště musí být konfigurace **`Handy (telefon)`** / modul **`app`**, **ne** `wear` ani „Handy Wear“.
   - Bílá obrazovka s textem *„Handy companion…“* = omylem spuštěný modul **`:wear`** (placeholder pro hodinky).
   - Správná app v launcheru: **Handy** (`cz.handy.app`). Wear: **Handy Wear (jen hodinky)**.

Při **prvním buildu** Gradle na Windows automaticky stáhne **český Vosk** + **Silero VAD** (~70 MB, může trvat pár minut, potřebuješ internet). Další buildy už jen sync → build.

## Volitelné (doporučené)

| Co | Kde |
|----|-----|
| Wake word | `local.properties` → `picovoice.access.key=…` z [console.picovoice.ai](https://console.picovoice.ai/) |
| Ověření hlasu (ECAPA) | Terminál: `.\gradlew.bat downloadHandyOnnxDevAssets` (potřebuje Python) |
| ONNX verze | ECAPA (`onnxruntime-android`) musí být **1.17.1** — stejně jako uvnitř sherpa-onnx; jinak zápis hlasu: `dlopen … OrtGetApiBase` |
| Kontrola assetů | `.\verify-assets.ps1` z této složky |

## APK na telefon

- Výstup: `Handy\app\build\outputs\apk\debug\app-debug.apk`
- Před testem **odinstaluj** starou Handy (nebo vymaž data), ať nepřežije starý ASR model.
- **Testuj debug ze Studia (Run)** — release APK má minifikaci; bez ONNX ProGuard pravidel padá zápis hlasu (`OrtSession.SessionOptions`).

## Jak mluvit s asistentem

| Režim | Wake-word | Příkazy |
|-------|-----------|---------|
| Bez `picovoice.access.key` | **Vypnutý** — nic tě neprobudí slovem | Klepni **Začít poslouchat**, pak česky např. *kolik je hodin*, *zhasni světlo* |
| S Picovoice klíčem | Vestavěné anglické **„Porcupine“** (ježek) | Po wake-word mluv příkaz česky |
| Debug build (Run ve Studiu) | Stejné | Navíc pole **Vyhodnotit příkaz** — český text bez zápisu hlasu |

**Zápis hlasu:** všechny věty Nahrát → Zastavit → **Uložit profil hlasu**. Bez uloženého profilu release build hlasové příkazy odmítne.

**Fáze dialogu Idle** je normální v klidu — po příkazu se změní (NLU / potvrzení).

## JDK / SDK

- Gradle JDK **17**
- Android SDK **API 35**, Build-Tools **35**

## Co je tato složka `Android apk`

Návod, checklist (`ASSETS_CHECKLIST.md`), `verify-assets.ps1` — **není** samostatný Gradle projekt.

Repo: https://github.com/bobthesmash/Handy
