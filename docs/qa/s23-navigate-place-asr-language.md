# S23 live check — NAVIGATE place ASR language (v0.2.6)

Device QA for location-selected recognition of the `{place}` slot after an English `navigate to` command. Typed console must keep working. Contact/place **names stay exact** (no fuzzy city “fixes”).

Build: **versionName 0.2.6 / versionCode 208**.

## How language is chosen (no GPS required in CI)

Logcat filter **`HandyPlaceAsr`**. First hit wins:

1. Last-known location (needs `ACCESS_COARSE_LOCATION`) → offline CZ/SK bounding box → Czech, otherwise English.
2. Telephony **network** country ISO (serving cell).
3. **SIM** country ISO.
4. Device **locale** country.
5. English fallback if every signal is blank.

Missing location permission must **not** crash. Expect a log like `skip last-known location: ACCESS_COARSE_LOCATION not granted — using network/SIM/locale`.

## Engines

- Command ASR: English Sherpa zipformer (`navigate to …`).
- Place tail in **CZ/SK**: Czech **Vosk** small on PCM after the command prefix (or a word-count estimate of that tail).
- Outside CZ/SK: English place string from command ASR, passed to Maps as spoken.

## A — Czechia / Slovakia

On S23 in CZ (or SK), with Vosk CZ bundled:

1. Say `navigate to` then a **Czech** place (e.g. `Karlův most`, `Brno hlavní nádraží`, a nearby street).
2. Logcat: `language=CZECH` and `Czech place tail='…'`.
3. NLU `NAVIGATE` `{place}` should be that Czech string (not an English-mangled toponym).
4. Google Maps must start **on-road guidance** (`google.navigation:q=…&mode=d` via lockscreen trampoline), same as v0.2.5. Confirm voice guidance starts.

Typed `navigate to austin` in the debug console must still navigate to **austin** (no GPS in that path).

## B — English-speaking area

In London / US (or with network ISO `GB`/`US` and no CZ last-known fix):

1. Say `navigate to` + an English place.
2. Logcat: `language=ENGLISH`.
3. Maps starts turn-by-turn with that English `{place}`.

## C — No location permission

Deny coarse location. Assistant must still run. Language comes from network/SIM/locale. If those are CZ/SK, Czech place capture still applies; otherwise English.
