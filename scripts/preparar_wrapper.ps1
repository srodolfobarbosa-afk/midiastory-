# Confere o Gradle Wrapper 8.13 no Windows sem regenerar arquivos.
$ErrorActionPreference = "Stop"
Write-Host "=== Verificando Gradle Wrapper ===" -ForegroundColor Cyan
if (-not (Test-Path ".\gradlew.bat") -or -not (Test-Path ".\gradle\wrapper\gradle-wrapper.jar")) {
    throw "gradlew.bat ou gradle-wrapper.jar ausente."
}
$properties = Get-Content ".\gradle\wrapper\gradle-wrapper.properties" -Raw
if ($properties -notmatch 'gradle-8\.13-(bin|all)\.zip') {
    throw "O wrapper não está configurado para Gradle 8.13."
}
Write-Host "Gradle Wrapper 8.13 verificado; nenhum arquivo foi alterado." -ForegroundColor Green
