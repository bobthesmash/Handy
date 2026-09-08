# S23 live check — ASR spelling + media duck (v0.2.5)

Device QA for the EN demo pipeline (`cz.handy.app`). Typed console must keep working.

Build: **versionName 0.2.5 / versionCode 207**.

## A — Fuzzy command phrases (~20% spelling)

Sherpa often emits near-misses (`BUTTER`, `BATTERET`, `FLESH LIGHT`, `LIGHT OF`, `OH LIGHT OFF`). Rule NLU uses **FuzzyWuzzy / Rhasspy `fuzz.ratio` + `token_sort_ratio`** with **min_confidence 80** (~20% spelling). Compound ASR splits (`flesh light`) are scored with spaces removed using the same ratio. WRatio/partial/tokenSet are not used (they would map `battery status` onto `battery`). Leading `OH`/`HEY` is stripped. **Contact and place names stay exact** (a wrong name must not be “corrected”).

Maven FuzzyKot (`com.github.terrakok:fuzzykot`) is Kotlin 2.3 metadata and does not load on Handy’s Kotlin 2.1 toolchain; the scorer is a tiny local copy of the same ratio/tokenSortRatio formulas.

Production parser always includes `HandyNluCatalogs.enMinimal` (play / pause / torch / battery / volume must stay in that catalog). Spoken standby is exactly **Mute** (TTS “See you”); wake is **Wake up Handy** (TTS “Hi”). Media STOP/pause must not use the word `mute`.

| Say / type | Expect |
|------------|--------|
| typed `battery` | `WHAT_BATTERY` (unchanged console path) |
| spoken `battery` heard as `BUTTER` / `BATTERET` / `WHAT IS MY BUTTER` | `WHAT_BATTERY`, not `NLU NoMatch` |
| `FLESH LIGHT` | `TORCH` on |
| `OH LIGHT OFF` / `LIGHT OF` | `TORCH` off |
| `call jane` / `call mitchell` | `CALL` with **that** name — not john/michael |
| `navigate to austin` | `NAVIGATE` place=`austin`, not boston |

Logcat: NLU match vs `NLU: nerozumím.`

## B — Pause while music plays (gated ~2 s duck)

Music lyrics/noise must **not** pause playback by themselves. Sherpa will still emit garbage partials (`AND A GOLD BRIMUS`, `GO THEY MUSTN'T`); those are ignored unless they look like a real command onset.

Duck gating (`SpeechOnsetDuckPolicy`, logcat **`HandyMediaDuck`**):

1. Only while **listening for a command** (not standby Mute, not while Handy TTS is speaking).
2. Text long enough (min 4 chars) and, when Sherpa `ysProbs` exist, **min token prob ≥ 0.40**. Rapid lyric-like lines do **not** duck. Short lyric words (`baby`, `yeah`, `love`, `tonight`) do **not** duck. Duck starts only when a **command token** appears (`pause`, `stop`, `play`, `battery`, `navigate`, …). Standby **Mute** is not a duck token.
3. A real short command (`pause`, `stop`, `play`, `battery`, …) with high confidence **does** pause active media for 2000 ms so ASR can hear the rest.
4. If the resolved intent is pause/STOP, duck **keeps paused** (window must not resume over a real pause/stop). Other intents resume.

Retest:

1. Enable Handy **notification listener** (Settings → Notifications → Device & app notifications / Notification access). Without it, Samsung logs `hasn't granted MEDIA_CONTENT_CONTROL` and pause/stop cannot see Spotify/YTM sessions — play may still work via app launch. Failure is spoken: enable notification access. Logcat **`HandyMediaSession`** / **`HandyMediaDuck`**.
2. `PLAY` so media is actually playing. Leave music on; Handy must **not** go silent from the music alone (no duck loop).
3. Speak `pause` (or `stop`). Playback pauses and **stays** paused.
4. `PLAY` again to resume, then `navigate to <a real nearby place>`.

## C — Navigate starts guidance

`navigate to <place>` must launch **on-road turn-by-turn** (`google.navigation:q=…&mode=d` via lockscreen trampoline), not a pin / directions list the user has to tap Start on. If that URI has no handler, fallback is Maps URLs `dir_action=navigate` (still navigate, never `geo:` preview). Confirm Google Maps actually starts voice guidance.
