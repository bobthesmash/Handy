ECAPA ONNX (192-D embedding)
================================

Place a compatible SpeakerBrain-style ECAPA export here as:

  ecapa_embedding.onnx
Place a compatible SpeechBrain-style ECAPA export here as:
Expected runtime contract (implemented in code):
 - Input float32 shaped [batch, time, mel] or [batch, mel, time] with mel dim = 80.
 - Output floatTensor with at least 192 elements (speaker embedding); first 192 used.
 - Frontend matches SpeechBrain Fbank defaults: 16 kHz mono, hop ~10 ms, win ~25 ms, n_fft=400, n_mels=80.


Anti-spoof ONNX (binary CM, optional)
======================================

Optional replay/synth classifier as:

  anti_spoof.onnx

When present it runs **before** ECAPA on every speaker-gated turn (phrase gate + destructive confirm).
Uses the **same log-mel** front-end as `ecapa_embedding.onnx`.

Output conventions (implemented in code):
 - First output slice: scalar logit interpreted as sigmoid P(spoof), **or**
 - At least two values: softmax(logit[0]=bonafide, logit[1]=spoof) → second mass is P(spoof).

Reject if P(spoof) > debug/store threshold (`antiSpoofRejectAbove`), default 0.5.

See docs/decisions/0007-anti-spoofing-onnx.md.

