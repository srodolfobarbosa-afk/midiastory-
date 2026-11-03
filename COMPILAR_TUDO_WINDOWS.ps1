# Script Mestre do Projeto Sweep v2.1.0 (versionCode = 13)
$ErrorActionPreference = "Stop"
& "$PSScriptRoot\scripts\verificar_ambiente.ps1"
& "$PSScriptRoot\scripts\preparar_wrapper.ps1"
& "$PSScriptRoot\scripts\executar_testes.ps1"
& "$PSScriptRoot\scripts\gerar_apk_debug.ps1"
& "$PSScriptRoot\scripts\preparar_release.ps1"
& "$PSScriptRoot\scripts\gerar_relatorio_qa.ps1"
Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "CONCLUIDO COM SUCESSO! OS DOIS ARQUIVOS .AAB (VERSAO 13) ESTAO NA PASTA DOWNLOADS!" -ForegroundColor Green
Write-Host "==================================================================" -ForegroundColor Cyan
