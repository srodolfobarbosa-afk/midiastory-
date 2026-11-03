# Calcula os hashes SHA-256 dos artefatos gerados na pasta Downloads
$ErrorActionPreference = "Stop"
Write-Host "=== [7/7] Relatório de Artefatos e SHA-256 ===" -ForegroundColor Cyan
$files = @(
    "$env:USERPROFILE\Downloads\SWEEP_2.1.0_VC13_OFICIAL.aab",
    "$env:USERPROFILE\Downloads\MIDIASTORE_2.1.0_VC13_OFICIAL.aab",
    "$env:USERPROFILE\Downloads\SWEEP_2.1.0_VC13_DEBUG.apk"
)
foreach ($f in $files) {
    if (Test-Path $f) {
        $hash = (Get-FileHash $f -Algorithm SHA256).Hash.ToLower()
        Write-Host "$hash  $f" -ForegroundColor Green
    }
}
