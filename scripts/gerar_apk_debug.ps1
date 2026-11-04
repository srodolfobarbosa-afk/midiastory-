# Gera APK Debug para teste interno/local do Midiastore.
# Debug usa applicationIdSuffix ".debug" e não pode atualizar a instalação da Play.
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Write-Host "=== Gerando APK Debug do Midiastore ===" -ForegroundColor Cyan
Push-Location $projectRoot
try {
    & "$projectRoot\gradlew.bat" --no-daemon :app:assembleDebug
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao gerar APK Debug (exit code $LASTEXITCODE)."
    }

    $apkPath = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apkPath)) {
        throw "APK Debug não encontrado em $apkPath."
    }

    $downloads = Join-Path $env:USERPROFILE "Downloads"
    New-Item -ItemType Directory -Force -Path $downloads | Out-Null
    $dest = Join-Path $downloads "MIDIASTORE_2.2.1_VC17_DEBUG.apk"
    Copy-Item $apkPath $dest -Force
    $hash = (Get-FileHash $dest -Algorithm SHA256).Hash.ToLowerInvariant()
    Write-Host "APK Debug copiado para: $dest" -ForegroundColor Green
    Write-Host "SHA-256: $hash" -ForegroundColor Green
    Write-Host "Package Debug esperado: com.rdf112.midiastore.debug" -ForegroundColor Yellow
}
finally {
    Pop-Location
}
