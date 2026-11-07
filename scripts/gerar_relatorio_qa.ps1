# Calcula SHA-256 do APK Debug do Midiastore, se ele existir.
$ErrorActionPreference = "Stop"
Write-Host "=== Relatório do APK Debug Midiastore ===" -ForegroundColor Cyan
$apk = Join-Path $env:USERPROFILE "Downloads\MIDIASTORE_2.2.1_VC17_DEBUG.apk"
if (Test-Path $apk) {
    $item = Get-Item $apk
    $hash = (Get-FileHash $apk -Algorithm SHA256).Hash.ToLowerInvariant()
    Write-Host "Arquivo: $($item.FullName)" -ForegroundColor Green
    Write-Host "Tamanho: $($item.Length) bytes" -ForegroundColor Green
    Write-Host "SHA-256: $hash" -ForegroundColor Green
    Write-Host "Este é um APK Debug; não é um artefato de produção." -ForegroundColor Yellow
} else {
    Write-Host "APK Debug não encontrado. Nenhum AAB foi gerado por esta rotina." -ForegroundColor Yellow
}
