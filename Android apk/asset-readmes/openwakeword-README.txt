ONNX weights for xyz.rementia OpenWakeWord (Apache-2.0 stack — see upstream openWakeWord + Re-MENTIA).

Place these files next to this README in assets/openwakeword/:

- melspectrogram.onnx
- embedding_model.onnx
- hey_handy.onnx   (your trained classifier; rename matches OpenWakeWordEngineFactory.FILE_KEYWORD or adjust code)

Training / export:
- David Scripka openWakeWord: https://github.com/dscripka/openWakeWord
- Kotlin binding: https://github.com/Re-MENTIA/openwakeword-android-kt

Without these assets, linkage is compiled but WakeWordEnginesProbe only logs "missing bundle".

Production note (ADR 0001):
- xyz.rementia WakeWordEngine.start() opens its own AudioRecord inside the library.
- Handy's EarService already owns the mic via one recorder + MonoPcmRingBuffer; Porcupine wake feeds from that bridge.
  Feeding the same PCM into openWakeWord requires upstream/fork API — do not assume you can mux both engines without two recorders unless you verified OEM behavior.
