# ASR confidence gate — hot JSON override

On-device Sherpa `ysProbs` often arrive as **log-probabilities** (negative, e.g. `-0.602`).
Comparing them raw against `minProb = 0.28` drops every spoken command while typed console
input (`minTokenProb = null`) still works.

## Built-in default (no file)

- Gate **enabled**, `mode = auto` (not a JSON value): if the score is outside `[0, 1]`, treat it
  as a log-prob (`exp`), then compare to `minProb` **in probability space**.
- Default `minProb = 0.28`.
- Missing or invalid `asr_gate.json` → these defaults. The assistant must not crash.

## File locations

Read on each utterance, re-parsed when the file mtime changes.

1. Preferred (adb-pushable, no root):

   `{app.getExternalFilesDir(null)}/asr_gate.json`

2. Fallback:

   `{app.filesDir}/asr_gate.json`

Package id is `cz.handy.app`, so the preferred path on device is typically:

```text
/sdcard/Android/data/cz.handy.app/files/asr_gate.json
```

## Schema

```json
{
  "enabled": true,
  "mode": "prob",
  "minProb": 0.28
}
```

| Field | Type | Meaning |
| --- | --- | --- |
| `enabled` | bool | `false` → never drop on confidence |
| `mode` | `"prob"` \| `"logprob"` \| `"off"` | see below |
| `minProb` | float | threshold in **probability** space (default `0.28`) |

Omitted or unparsable fields keep the built-in default for that field.

### `mode`

- `"prob"` — compare the raw Sherpa score to `minProb` (use when scores are already in `[0, 1]`).
- `"logprob"` — always `exp(score)`, then compare to `minProb`.
- `"off"` — never drop on confidence (same as `enabled: false`).
- missing / unknown — same as no file: **auto** (exp when outside `[0, 1]`).

## adb push example

Disable the gate while tuning:

```json
{"enabled": true, "mode": "off", "minProb": 0.28}
```

```bash
adb push asr_gate.json /sdcard/Android/data/cz.handy.app/files/asr_gate.json
```

No APK rebuild. The next utterance reloads the file if mtime changed.

To restore defaults, delete the file:

```bash
adb shell rm /sdcard/Android/data/cz.handy.app/files/asr_gate.json
```
