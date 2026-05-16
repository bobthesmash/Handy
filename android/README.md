## Handy Android Studio (podsložka `android`)

Tento projekt má **dvě platné Gradle root**:

| Otevřít v IDE | Kdy použít |
|---------------|-------------|
| `Handy/` (kořen repozitáře) | Výchozí — příkaz `./gradlew` v CI odkazuje sem. |
| `Handy/android/` | Stačí jen Android Studio bez „nadbytečných“ adresářů v šetřenci; používá stejné moduly (`app`, `core/*`, `feature/*`) přes relativní `projectDir`. |

### Postup ve studiu `android/`

1. **File → Open** a vyberte složku `Handy/android` (musí tam být `settings.gradle.kts`).
2. **SDK:** Gradle v této složce hledá `android/local.properties` (nezávisle na kořeni repa). Nejrychlejší je zkopírovat `local.properties` z kořene `Handy/` do `Handy/android/`, případně nechat doplnění ve studiu.
3. Vytvořte `local.properties`, pokud studium neudělá samo, např.:
   ```
   sdk.dir=C\:\\Users\\VášeJméno\\AppData\\Local\\Android\\Sdk
   ```
   (Windows — escapované zpětné lomítka; šablona v `local.properties.example`.)
4. Sync Gradle pak **Run** modul `:app`.

### Příkazová řádka ze složky `android/`

```bat
.\gradlew.bat :app:assembleDebug
```

### Poznámka

Gradle wrapper (`gradlew`, `gradle/wrapper/`) je v `android/` záměrně duplicitní s kořenem, aby build šel lokálně i bez šetření do nadřazené složky. Katalog závislostí **`gradle/libs.versions.toml`** čte `settings.gradle.kts` z nadřazeného `Handy/gradle/` — celý git repozitář má být k dispozici (ne jen samostatný export `android/`).
