Český Vosk small (Rhasspy) — produkční ASR pro Handy
====================================================

Stažení (z kořene repa):

  powershell -ExecutionPolicy Bypass -File scripts/download-handy-onnx-assets.ps1

Očekávaná struktura v tomto adresáři (po rozbalení zipu):

  am/final.mdl
  conf/
  graph/

Zdroj: https://huggingface.co/rhasspy/vosk-models/tree/main/cs (vosk-model-small-cs-0.4-rhasspy.zip)

Aplikace preferuje tento model před Sherpa zipformer2 v `cs_zipformer_small/`
(ruský vývojářský placeholder). Nepřidávej sem ruční kopii — použij skript výše.
