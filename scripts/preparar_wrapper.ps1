# Prepara e verifica o Gradle Wrapper no Windows
$ErrorActionPreference = "Stop"
Write-Host "=== [2/7] Verificando Gradle Wrapper ===" -ForegroundColor Cyan
if (-not (Test-Path "gradlew.bat") -or -not (Test-Path "gradle\wrapper\gradle-wrapper.jar")) {
    Write-Host "ERRO: gradlew.bat ou gradle-wrapper.jar ausente." -ForegroundColor Red
    exit 1
}
Write-Host "Gradle Wrapper 8.13 pronto para execução." -ForegroundColor Green
