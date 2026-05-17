# Kontrola chybějících ONNX/LLM assetů v nadřazeném repu Handy/
$ErrorActionPreference = "Continue"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Write-Host "Repo: $repoRoot" -ForegroundColor Cyan

$required = @(
    @{ Label = "ASR Vosk CS am/final.mdl"; Path = "feature/asr/src/main/assets/asr/vosk_cs_small/am/final.mdl" },
    @{ Label = "ASR tokens.txt (Sherpa záloha)"; Path = "feature/asr/src/main/assets/asr/cs_zipformer_small/tokens.txt" },
    @{ Label = "ASR encoder.onnx"; Path = "feature/asr/src/main/assets/asr/cs_zipformer_small/encoder.onnx" },
    @{ Label = "ASR decoder.onnx"; Path = "feature/asr/src/main/assets/asr/cs_zipformer_small/decoder.onnx" },
    @{ Label = "ASR joiner.onnx"; Path = "feature/asr/src/main/assets/asr/cs_zipformer_small/joiner.onnx" },
    @{ Label = "ECAPA ecapa_embedding.onnx"; Path = "feature/voiceid/src/main/assets/voiceid/ecapa_embedding.onnx" },
    @{ Label = "Silero silero_vad.onnx"; Path = "feature/voiceid/src/main/assets/voiceid/silero_vad.onnx" }
)

$optional = @(
    @{ Label = "Anti-spoof anti_spoof.onnx"; Path = "feature/voiceid/src/main/assets/voiceid/anti_spoof.onnx" },
    @{ Label = "openWakeWord melspectrogram.onnx"; Path = "feature/wakeword/src/main/assets/openwakeword/melspectrogram.onnx" },
    @{ Label = "openWakeWord embedding_model.onnx"; Path = "feature/wakeword/src/main/assets/openwakeword/embedding_model.onnx" },
    @{ Label = "openWakeWord hey_handy.onnx"; Path = "feature/wakeword/src/main/assets/openwakeword/hey_handy.onnx" },
    @{ Label = "MediaPipe gemma_hand_task.task"; Path = "feature/nlu-llm/src/main/assets/nlu_llm/gemma_hand_task.task" }
)

$localProps = Join-Path $repoRoot "local.properties"
if (Test-Path $localProps) {
    $content = Get-Content $localProps -Raw
    if ($content -match "picovoice\.access\.key\s*=\s*\S+") {
        Write-Host "[OK] picovoice.access.key v local.properties" -ForegroundColor Green
    } else {
        Write-Host "[WARN] local.properties existuje, ale chybí picovoice.access.key (wake word)" -ForegroundColor Yellow
    }
} else {
    Write-Host "[WARN] Chybí Handy/local.properties (sdk.dir + volitelně picovoice)" -ForegroundColor Yellow
}

function Test-AssetList($items, [string]$kind) {
    foreach ($item in $items) {
        $full = Join-Path $repoRoot $item.Path
        if (Test-Path $full -PathType Leaf) {
            $mb = [math]::Round((Get-Item $full).Length / 1MB, 2)
            Write-Host "[OK] $($item.Label) ($mb MB)" -ForegroundColor Green
        } else {
            Write-Host "[$kind] $($item.Label) -> $($item.Path)" -ForegroundColor $(if ($kind -eq "MISSING") { "Red" } else { "DarkYellow" })
        }
    }
}

Write-Host "`n--- Povinné pro plný hlasový stack ---" -ForegroundColor Cyan
Test-AssetList $required "MISSING"

Write-Host "`n--- Volitelné (F5 / experiment) ---" -ForegroundColor Cyan
Test-AssetList $optional "optional"

$apkKit = Join-Path $PSScriptRoot "apk\app-debug.apk"
$apkBuild = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkKit) {
    $mb = [math]::Round((Get-Item $apkKit).Length / 1MB, 1)
    Write-Host "`n[OK] Hotové APK v této složce: apk\app-debug.apk ($mb MB)" -ForegroundColor Green
}
if (Test-Path $apkBuild) {
    Write-Host "[OK] APK z posledního buildu: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Green
}
