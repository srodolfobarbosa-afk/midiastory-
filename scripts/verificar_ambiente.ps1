# Verifica pré-requisitos do ambiente Windows para o projeto Sweep
$ErrorActionPreference = "Stop"
Write-Host "=== [1/7] Verificando ambiente Windows para o Sweep ===" -ForegroundColor Cyan

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Write-Host "ERRO: Java (JDK 17) não encontrado no PATH." -ForegroundColor Red
    exit 1
}
Write-Host "Java encontrado: $($javaCmd.Source)" -ForegroundColor Green

if (Test-Path "local.properties") {
    Write-Host "local.properties encontrado." -ForegroundColor Green
} else {
    Write-Host "AVISO: local.properties não encontrado na pasta atual." -ForegroundColor Yellow
}

if ((Test-Path "sweep-upload.jks") -or (Test-Path "app\sweep-upload.jks")) {
    Write-Host "Chave de assinatura sweep-upload.jks encontrada." -ForegroundColor Green
} else {
    Write-Host "AVISO: sweep-upload.jks não encontrada na pasta atual." -ForegroundColor Yellow
}
