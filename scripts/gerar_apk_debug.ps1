# Gera APK Debug instalável para teste rápido no celular
$ErrorActionPreference = "Stop"
Write-Host "=== [4/7] Gerando APK Debug (:app:assembleDebug) ===" -ForegroundColor Cyan
cmd /c "call gradlew.bat --no-daemon :app:assembleDebug"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Falha ao gerar APK Debug." -ForegroundColor Red
    exit $LASTEXITCODE
}
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $dest = "$env:USERPROFILE\Downloads\SWEEP_2.1.0_VC13_DEBUG.apk"
    Copy-Item $apkPath $dest -Force
    Write-Host "APK Debug copiado para: $dest" -ForegroundColor Green
}
