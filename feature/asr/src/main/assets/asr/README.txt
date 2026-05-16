Streaming zipformer2 (CZ / náhradní jazyk)
============================================

Adresář (relativně k `assets/`):

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

Upstream: https://k2-fsa.github.io/sherpa/onnx/pretrained_models/index.html
