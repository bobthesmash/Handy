# ASR confidence gate — hot JSON tuner

On-device knobs for the Sherpa token-confidence gate, so we can accept/reject
utterances without rebuilding the APK. The core pipeline (mic → ASR → NLU →
actions) stays in the APK; this file only overrides the gate.

## File location

Read **each utterance**, re-parsing when the path or mtime changes.

1. **Primary (adb-pushable, no root):** `Context.getExternalFilesDir(null)/asr_gate.json`
2. **Fallback:** `Context.filesDir/asr_gate.json`

For `applicationId` `cz.handy.app` the primary path is:

```text
/sdcard/Android/data/cz.handy.app/files/asr_gate.json
```

Missing, unreadable, or invalid JSON → built-in defaults (never crash).

## Schema

```json
{
  "enabled": true,
  "mode": "prob",
  "minProb": 0.28
}
```

| Field | Type | Default (no file / omitted) | Meaning |
| --- | --- | --- | --- |
| `enabled` | bool | `true` | `false` → never drop on confidence |
| `mode` | string | auto-detect | `prob` \| `logprob` \| `off` |
| `minProb` | float | `0.28` | Threshold in **probability** space |

### Modes

- **`off`** (or `enabled: false`): gate disabled; spoken commands always proceed (typed console already passes `minTokenProb=null` and skips the gate).
- **`prob`**: compare the raw Sherpa score to `minProb` (use this if `ysProbs` are already in `[0, 1]`).
- **`logprob`**: always `exp(score)` then compare to `minProb`.
- **Built-in default (no file, or `mode` omitted):** treat a score as a log-prob when it is **outside `[0, 1]`** (`exp`), otherwise leave it as a probability. On-device Sherpa `ysProbs` have been observed as negatives (e.g. `minTokenProb=-0.602`); comparing that raw value to `0.28` silent-drops every utterance.

Unknown `mode` strings fall back to auto-detect. Extra JSON keys are ignored.

## adb push example

```bash
cat > asr_gate.json <<'EOF'
{
  "enabled": true,
  "mode": "logprob",
  "minProb": 0.28
}
EOF

adb push asr_gate.json /sdcard/Android/data/cz.handy.app/files/asr_gate.json
```

Disable the gate entirely while debugging NLU:

```bash
echo '{"enabled":false,"mode":"off"}' > asr_gate.json
adb push asr_gate.json /sdcard/Android/data/cz.handy.app/files/asr_gate.json
```

No app restart is required: the next utterance re-reads the file if mtime changed.
Look for logcat tag `HandyAsrGate` (`FINAL ASR`, `drop: low ASR confidence`, config reload).
