Handy F5-T04 — Piper (offline český TTS)

Do této složky patří ONNX Piper model + konfigurace hlasu podle upstream Piper dokumentace.
Binárky jsou záměrně mimo git kvůli velikosti licence.

Integrace v aplikaci: rozhraní zůstává [AndroidCzechSpeechSynthesizer] dokud nevznikne nativní runtime
(`onnxruntime` nebo vestavěný Piper JNI) a napojení na [SpeechSynthesizer].

Do té doby slouží tento README jako podklad pro ruční sestavení a QA.
