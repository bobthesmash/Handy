# Checklist assetů (relativně ke kořeni `Handy/`)

Zkopíruj soubory do přesných cest. **Nechceš je commitovat** (velikost / licence). Po zkopírování spusť `verify-assets.ps1`.

## Povinné pro plný běh (MVP hlas)

| Soubor | Cílová cesta v repu |
|--------|---------------------|
| `tokens.txt` | `feature/asr/src/main/assets/asr/cs_zipformer_small/tokens.txt` |
| `encoder.onnx` | `feature/asr/src/main/assets/asr/cs_zipformer_small/encoder.onnx` |
| `decoder.onnx` | `feature/asr/src/main/assets/asr/cs_zipformer_small/decoder.onnx` |
| `joiner.onnx` | `feature/asr/src/main/assets/asr/cs_zipformer_small/joiner.onnx` |
| `ecapa_embedding.onnx` | `feature/voiceid/src/main/assets/voiceid/ecapa_embedding.onnx` |
| `silero_vad.onnx` | `feature/voiceid/src/main/assets/voiceid/silero_vad.onnx` |

**ASR:** streaming zipformer2 transducer kompatibilní se Sherpa-onnx — viz `asset-readmes/asr-README.txt` a https://k2-fsa.github.io/sherpa/onnx/pretrained_models/

**ECAPA / VAD:** viz `asset-readmes/voiceid-README.txt`, ADR `docs/decisions/0002-*.md`, `0003-*.md`.

## Konfigurace (ne asset soubor)

| Položka | Kde |
|---------|-----|
| `picovoice.access.key` | `Handy/local.properties` (viz `START_HERE.md`) |

## Volitelné (F5 / experimenty)

| Soubor | Cílová cesta | Poznámka |
|--------|--------------|----------|
| `anti_spoof.onnx` | `feature/voiceid/src/main/assets/voiceid/` | Před ECAPA; ADR-0007 |
| `melspectrogram.onnx`, `embedding_model.onnx`, `hey_handy.onnx` | `feature/wakeword/src/main/assets/openwakeword/` | openWakeWord; viz readme |
| `gemma_hand_task.task` | `feature/nlu-llm/src/main/assets/nlu_llm/` | MediaPipe LLM; bez něj jen pravidla |
| Piper ONNX + config | `feature/tts/src/main/assets/piper/` | F5-T04; zatím Android TTS |

## Složky — vytvoř prázdné, pokud neexistují

```text
feature/asr/src/main/assets/asr/cs_zipformer_small/
feature/voiceid/src/main/assets/voiceid/
feature/wakeword/src/main/assets/openwakeword/
feature/nlu-llm/src/main/assets/nlu_llm/
feature/tts/src/main/assets/piper/
```

## Ověření po doplnění

```bat
cd D:\Handy
.\gradlew.bat checkHandyOnnxDevAssets
.\gradlew.bat :app:assembleDebug
```

Očekávaná velikost finálního APK: řádově **stovky MB** podle toho, kolik ONNX/LLM vložíš do assets.
