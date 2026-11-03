# Valida a presença e impressões digitais (SHA-1 / SHA-256) do certificado de upload
$ErrorActionPreference = "Stop"
Write-Host "=== [5/7] Validando certificado sweep-upload.jks ===" -ForegroundColor Cyan
$jks = if (Test-Path "sweep-upload.jks") { "sweep-upload.jks" } elseif (Test-Path "app\sweep-upload.jks") { "app\sweep-upload.jks" } else { $null }
if (-not $jks) {
    Write-Host "ERRO: Arquivo sweep-upload.jks não encontrado." -ForegroundColor Red
    exit 1
}
Write-Host "Certificado localizado em: $jks" -ForegroundColor Green
