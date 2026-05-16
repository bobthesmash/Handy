ECAPA ONNX (192-D embedding)
================================

Place a compatible SpeakerBrain-style ECAPA export here as:

  ecapa_embedding.onnx
Place a compatible SpeechBrain-style ECAPA export here as:
Expected runtime contract (implemented in code):
 - Input float32 shaped [batch, time, mel] or [batch, mel, time] with mel dim = 80.
 - Output floatTensor with at least 192 elements (speaker embedding); first 192 used.
 - Frontend matches SpeechBrain Fbank defaults: 16 kHz mono, hop ~10 ms, win ~25 ms, n_fft=400, n_mels=80.

The file is deliberately not committed (large/binary). Obtain or export offline from the
speechbrain pretrained family (see docs/decisions/0002-ecapa-speaker-onnx.md).

