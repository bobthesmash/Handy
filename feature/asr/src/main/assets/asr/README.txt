ASR modely (assets)
===================

**Produkce — čeština (Vosk), NAVIGATE place tail v CZ/SK:**

  asr/vosk_cs_small/     ← `am/`, `conf/`, `graph/` (viz `vosk_cs_small/README.txt`)

**Produkce — anglické příkazy (Sherpa zipformer transducer):**

  asr/cs_zipformer_small/


Povinné soubory (**přejmenuj** stažené ONNX na tyto názvy):

  tokens.txt
  encoder.onnx
  decoder.onnx
  joiner.onnx


Kontrakt je shodný se **sherpa-onnx** streaming transducer modely typu *zipformer2*
(např. struktura `sherpa-onnx-streaming-zipformer-small-ru-vosk-*` na Hugging Face).
Pro češtinu hledej dostupný vosk/icefall ONNX export se stejným rozhraním; dokud není,
můžeš do stejné složky dočasně dát jiný zipformer2 streaming balíček (např. malý EN)
pro smoke test — **ne commitovat** kvůli velikosti.

`navigate to {place}`: příkaz zůstává na anglickém Sherpa streamu; v CZ/SK se
dořeknuté místo dekóduje Vosk CZ (kaskáda, viz `docs/qa/s23-navigate-place-asr-language.md`).

Upstream: https://k2-fsa.github.io/sherpa/onnx/pretrained_models/index.html



Povinné soubory (**přejmenuj** stažené ONNX na tyto názvy):

  tokens.txt
  encoder.onnx
  decoder.onnx
  joiner.onnx


Kontrakt je shodný se **sherpa-onnx** streaming transducer modely typu *zipformer2*
(např. struktura `sherpa-onnx-streaming-zipformer-small-ru-vosk-*` na Hugging Face).
Pro češtinu hledej dostupný vosk/icefall ONNX export se stejným rozhraním; dokud není,
můžeš do stejné složky dočasně dát jiný zipformer2 streaming balíček (např. malý EN)
pro smoke test — **ne commitovat** kvůli velikosti.

Upstream: https://k2-fsa.github.io/sherpa/onnx/pretrained_models/index.html
