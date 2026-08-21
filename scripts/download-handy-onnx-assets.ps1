# Downloads ONNX assets required for full local voice stack (ASR + VAD + ECAPA).
# Run from repo root:  powershell -ExecutionPolicy Bypass -File scripts/download-handy-onnx-assets.ps1

param(
    [switch]$ForceAsr,
    [switch]$SkipSherpa,
    [switch]$SkipEcapa
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

function Get-FileIfMissing {
    param(
        [string]$Url,
        [string]$Dest,
        [string]$Label,
        [switch]$Force
    )
    if ((Test-Path $Dest) -and -not $Force) {
        $mb = [math]::Round((Get-Item $Dest).Length / 1MB, 2)
        Write-Host "[skip] $Label already present ($mb MB)" -ForegroundColor DarkGray
        return
    }
    $dir = Split-Path -Parent $Dest
    if ($dir -and -not (Test-Path $dir)) {
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
    }
    Write-Host "[get]  $Label ..." -ForegroundColor Cyan
    Write-Host "       $Url"
    Invoke-WebRequest -Uri $Url -OutFile $Dest -UseBasicParsing
    $mb = [math]::Round((Get-Item $Dest).Length / 1MB, 2)
    Write-Host "[ok]   $Label ($mb MB)" -ForegroundColor Green
}

Write-Host "Handy ONNX assets -> $Root" -ForegroundColor Cyan

$VoskZipUrls = @(
    "https://huggingface.co/rhasspy/vosk-models/resolve/main/cs/vosk-model-small-cs-0.4-rhasspy.zip",
    "https://alphacephei.com/vosk/models/vosk-model-small-cs-0.4-rhasspy.zip"
)
$VoskAssetDir = Join-Path $Root "feature\asr\src\main\assets\asr\vosk_cs_small"
$VoskMarker = Join-Path $VoskAssetDir "am\final.mdl"
if ((Test-Path $VoskMarker) -and -not $ForceAsr) {
    Write-Host "[skip] Vosk CS model already present" -ForegroundColor DarkGray
} else {
    if (Test-Path $VoskAssetDir) {
        Write-Host "[vosk] Removing old Czech model tree..." -ForegroundColor DarkYellow
        Get-ChildItem -Path $VoskAssetDir -Exclude "README.txt" | Remove-Item -Recurse -Force
    }
    $tmpZip = Join-Path $env:TEMP "vosk-model-small-cs-0.4-rhasspy.zip"
    $tmpExtract = Join-Path $env:TEMP "vosk-cs-extract-$(Get-Random)"
    Write-Host "[get]  Vosk CS small (Rhasspy) ..." -ForegroundColor Cyan
    $downloaded = $false
    foreach ($url in $VoskZipUrls) {
        Write-Host "       $url"
        try {
            Invoke-WebRequest -Uri $url -OutFile $tmpZip -UseBasicParsing
            $downloaded = $true
            break
        } catch {
            Write-Host "       failed: $($_.Exception.Message)" -ForegroundColor DarkYellow
        }
    }
    if (-not $downloaded) {
        throw "Vosk CS download failed from all mirrors."
    }
    New-Item -ItemType Directory -Force -Path $tmpExtract | Out-Null
    Expand-Archive -Path $tmpZip -DestinationPath $tmpExtract -Force
    $inner = Get-ChildItem -Path $tmpExtract -Directory | Select-Object -First 1
    if (-not $inner) { throw "Vosk zip has no top-level directory." }
    New-Item -ItemType Directory -Force -Path $VoskAssetDir | Out-Null
    Copy-Item -Path (Join-Path $inner.FullName "*") -Destination $VoskAssetDir -Recurse -Force
    Remove-Item -Recurse -Force $tmpExtract -ErrorAction SilentlyContinue
    Remove-Item -Force $tmpZip -ErrorAction SilentlyContinue
    Write-Host "[ok]   Vosk CS -> $VoskAssetDir" -ForegroundColor Green
}

if (-not $SkipSherpa) {
    $SherpaBase = "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-small-ru-vosk-2025-08-16/resolve/main"
    $SherpaDir = Join-Path $Root "feature\asr\src\main\assets\asr\cs_zipformer_small"
    foreach ($name in @("tokens.txt", "encoder.onnx", "decoder.onnx", "joiner.onnx")) {
        Get-FileIfMissing -Url "$SherpaBase/$name" -Dest (Join-Path $SherpaDir $name) -Label "Sherpa RU placeholder $name" -Force:$ForceAsr
    }
} else {
    Write-Host "[skip] Sherpa zipformer (záložní ASR)" -ForegroundColor DarkGray
}

$VoiceDir = Join-Path $Root "feature\voiceid\src\main\assets\voiceid"
Get-FileIfMissing `
    -Url "https://github.com/snakers4/silero-vad/raw/master/src/silero_vad/data/silero_vad.onnx" `
    -Dest (Join-Path $VoiceDir "silero_vad.onnx") `
    -Label "Silero VAD v5"

$EcapaOut = Join-Path $VoiceDir "ecapa_embedding.onnx"
if ($SkipEcapa) {
    Write-Host "[skip] ECAPA export (spusť bez -SkipEcapa pro ověření hlasu)" -ForegroundColor DarkGray
} elseif (-not (Test-Path $EcapaOut)) {
    Write-Host "[ecapa] Export via SpeechBrain (deps on D: if C: is full)..." -ForegroundColor Cyan
    $pyDeps = Join-Path $Root "build\python-deps"
    $pipCache = Join-Path $Root "build\pip-cache"
    $tmpDir = Join-Path $Root "build\tmp"
    $sbCache = Join-Path $Root "build\speechbrain-spkrec-ecapa-voxceleb"
    foreach ($d in @($pyDeps, $pipCache, $tmpDir, $sbCache)) {
        New-Item -ItemType Directory -Force -Path $d | Out-Null
    }
    $env:PIP_CACHE_DIR = $pipCache
    $env:TMP = $tmpDir
    $env:TEMP = $tmpDir
    $req = Join-Path $PSScriptRoot "requirements-onnx-export.txt"
    python -m pip install --upgrade pip --target $pyDeps 2>$null | Out-Null
    python -m pip install --target $pyDeps -r $req
    if ($LASTEXITCODE -ne 0) {
        throw "pip install failed (need ~2 GB free on D: under Handy/build/)."
    }
    $env:PYTHONPATH = $pyDeps
    $export = Join-Path $PSScriptRoot "export_ecapa_embedding_onnx.py"
    python $export --out $EcapaOut --savedir $sbCache
    if ($LASTEXITCODE -ne 0) {
        throw "ECAPA export failed after pip install."
    }
} else {
    $mb = [math]::Round((Get-Item $EcapaOut).Length / 1MB, 2)
    Write-Host "[skip] ecapa_embedding.onnx already present ($mb MB)" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "Done. Verify:" -ForegroundColor Cyan
Write-Host "  .\gradlew.bat checkHandyOnnxDevAssets"
Write-Host "  (optional) Android apk\verify-assets.ps1"
