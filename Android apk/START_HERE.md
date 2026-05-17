# Handy — sestavení APK v Android Studiu

Tato složka doplňuje **git repozitář** `Handy/` (nadřazená složka). Samotná složka `Android apk` **není** kompletní projekt — kódu a Gradle modulů potřebuješ celý klon.

## Co je tady

| Položka | Účel |
|---------|------|
| `apk/app-debug.apk` | Hotový debug build (~140 MB) — rychlá instalace bez studia |
| `ASSETS_CHECKLIST.md` | Velké ONNX / modely — kam je zkopírovat v repu |
| `asset-readmes/` | Kopie README z modulů (ASR, voiceid, wake, LLM, Piper) |
| `local.properties.example` | Šablona cesty k Android SDK |
| `verify-assets.ps1` | Kontrola, které soubory v repu ještě chybí |
| `docs/COMPLETION_GUIDE.md` | Co zbývá k release (HW měření, beta, rozhodnutí) |

## 1. Android Studio — otevření projektu

**Doporučeno:** otevři kořen repozitáře:

1. **File → Open** → `D:\Handy` (složka s `settings.gradle.kts` a `gradlew.bat`).
2. JDK **17** (File → Settings → Build → Gradle → Gradle JDK).
3. **Android SDK:** API **35**, Build-Tools **35** (SDK Manager).
4. Vytvoř `local.properties` v kořeni `Handy/` (nebo zkopíruj z `local.properties.example`):

   ```properties
   sdk.dir=C\:\\Users\\TVOJE_JMENO\\AppData\\Local\\Android\\Sdk
   ```

5. **Sync Project with Gradle Files**, pak **Run** modul **app**.

**Alternativa:** jen podsložka `Handy/android/` — viz `docs/ANDROID_STUDIO_ANDROID_SUBFOLDER.md`. Celý git repozitář musí zůstat na disku (moduly jsou relativně nad `android/`).

## 2. Picovoice (wake word)

Do `Handy/local.properties` (necommitovat):

```properties
sdk.dir=...
picovoice.access.key=VÁŠ_KLÍČ_Z_console.picovoice.ai
```

Bez klíče se Porcupine pumpa nespustí; build projde. Výchozí keyword je vestavěné `PORCUPINE` (viz ADR-0001).

## 3. Velké modely (povinné pro plnou funkci)

Z kořene repa (Python 3 + pip):

```bat
cd ..\Handy
.\gradlew.bat downloadHandyOnnxDevAssets
```

Kontrola z této složky:

```powershell
.\verify-assets.ps1
```

Podrobná tabulka: **`ASSETS_CHECKLIST.md`**. Po stažení znovu **Build → Make Project** / `.\gradlew.bat :app:assembleDebug`.

## 4. Build příkazy

| Cíl | Příkaz |
|-----|--------|
| Debug APK | `.\gradlew :app:assembleDebug` |
| Release APK (R8) | `.\gradlew :app:assembleRelease` |
| CI ekvivalent | `.\gradlew ciHandy` |
| CI + release | `.\gradlew ciHandyFull` |

Výstup debug APK: `Handy/app/build/outputs/apk/debug/app-debug.apk` (stejný obsah jako `apk/app-debug.apk` v této složce, pokud jsi právě buildil).

## 5. Instalace na telefon

```bat
adb install -r "D:\Handy\Android apk\apk\app-debug.apk"
```

Nebo přetáhni APK do telefonu (povolit neznámé zdroje).

## 6. Wear (volitelné F5)

Samostatný modul: `.\gradlew :wear:assembleDebug` → `wear/build/outputs/apk/debug/wear-debug.apk`.

## 7. Co už v kódu je / co doplníš ty

- **V gitu:** aplikace, NLU, UI, Sherpa AAR z Mavenu, MediaPipe `tasks-genai` z Maven Central.
- **Mimo git (zkopíruješ podle checklistu):** ASR zipformer ONNX, ECAPA + Silero VAD, volitelně anti-spoof, openWakeWord, Gemma `.task`, Piper.
- **Jen na zařízení / v dokumentaci:** latence, baterie, 50+30 hlasových nahrávek, beta — viz `docs/COMPLETION_GUIDE.md`.

## 8. Experimentální F5 v aplikaci

**Nastavení** → blok experimentální (anglický NLU overlay, odkaz na accessibility). Lokální LLM funguje až s `gemma_hand_task.task` v assets (viz checklist).

---

Repo: https://github.com/bobthesmash/Handy — po `git pull` máš stejný kód jako na GitHubu.
