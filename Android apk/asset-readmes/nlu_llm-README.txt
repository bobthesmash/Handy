Handy F5-T01 — lokální LLM přes MediaPipe (tasks-genai)

Do této složky zkopíruj přejmenovaný multimediální balík modelu (např. Gemma 2B INT4 ve formátu `.task`
kompatibilním s MediaPipe LLM Inference API).

Kanické jméno souboru v APK assets:
  gemma_hand_task.task

Bez tohoto souboru zůstane MediaPipe větev NLU neaktivní a použijí se jen pravidla ([RuleBasedNluEngine]).

Velké binárky záměrně nejsou v gitu — viz ADR docs/decisions/0008-nlu-v2-llm-primary-rules-fallback.md
