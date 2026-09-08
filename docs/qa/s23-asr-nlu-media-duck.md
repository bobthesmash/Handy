# S23 live check — ASR spelling + media duck (v0.2.3)

Device QA for the EN demo pipeline (`cz.handy.app`). Typed console must keep working.

## A — Fuzzy command phrases (~20% spelling)

Sherpa often emits near-misses (`BUTTER`, `BATTERET`, `FLESH LIGHT`, `LIGHT OF`, `OH LIGHT OFF`). Rule NLU uses **FuzzyWuzzy / Rhasspy `fuzz.ratio` + `token_sort_ratio`** with **min_confidence 80** (~20% spelling). Compound ASR splits (`flesh light`) are scored with spaces removed using the same ratio. WRatio/partial/tokenSet are not used (they would map `battery status` onto `battery`). Leading `OH`/`HEY` is stripped. **Contact and place names stay exact** (a wrong name must not be “corrected”).

Maven FuzzyKot (`com.github.terrakok:fuzzykot`) is Kotlin 2.3 metadata and does not load on Handy’s Kotlin 2.1 toolchain; the scorer is a tiny local copy of the same ratio/tokenSortRatio formulas.

| Say / type | Expect |
|------------|--------|
| typed `battery` | `WHAT_BATTERY` (unchanged console path) |
| spoken `battery` heard as `BUTTER` / `BATTERET` / `WHAT IS MY BUTTER` | `WHAT_BATTERY`, not `NLU NoMatch` |
| `FLESH LIGHT` | `TORCH` on |
| `OH LIGHT OFF` / `LIGHT OF` | `TORCH` off |
| `call jane` / `call mitchell` | `CALL` with **that** name — not john/michael |
| `navigate to austin` | `NAVIGATE` place=`austin`, not boston |

Logcat: NLU match vs `NLU: nerozumím.`

## B — Pause while music plays (~2 s duck)

1. `PLAY` so media is actually playing.
2. Speak `pause` (or `stop`). First non-empty ASR partial should **pause/mute playback** so the rest of the command is audible.
3. Logcat filter **`HandyMediaDuck`**: pause for 2000 ms speech window; if intent is pause/stop, **keep paused**; otherwise resume.

If playback never ducks, confirm notification listener access (media sessions) is granted for Handy.
