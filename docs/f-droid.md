# F-Droid / reproducible build ([F5-T07])

Handy má `applicationId` **`cz.handy.app`** (`app/build.gradle.kts`).

## Stav integrace

- V repu je šablona metadat `metadata/cz.handy.app.yml` pro [F-Droid data](https://gitlab.com/fdroid/fdroiddata).
- Reálný merge do `fdroiddata` vyžaduje:
  - podepsaný release APK/AAB nebo tag s reproducible build instrukcemi,
  - vyplnění Anti-Features (senzitivní oprávnění),
  - kontrolu proprietárních závislostí (Porcupine licence — viz `docs/decisions/0001-wake-word.md`).

## Lokální checklist před žádostí o listing

1. `./gradlew ciHandyFull` zelený na čistém stroji.
2. Ověření, že žádný ONNX/asset neporušuje redistributabilitu tvého výběru modelů.
3. Privacy policy odkaz (`docs/legal/`).
