# Compila e valida o Midiastore para teste local no Windows.
# Este script NÃO gera AAB/Release e NÃO altera applicationId, versionCode ou assinatura.
$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot
Push-Location $projectRoot
try {
    & "$projectRoot\scripts\verificar_ambiente.ps1"
    & "$projectRoot\scripts\preparar_wrapper.ps1"
    & "$projectRoot\scripts\executar_testes.ps1"
    & "$projectRoot\scripts\gerar_apk_debug.ps1"
    & "$projectRoot\scripts\preparar_release.ps1"
    & "$projectRoot\scripts\gerar_relatorio_qa.ps1"

    Write-Host "==============================================================" -ForegroundColor Cyan
    Write-Host "VALIDAÇÃO DEBUG CONCLUÍDA. RELEASE/AAB NÃO FOI GERADO." -ForegroundColor Green
    Write-Host "Aplicativo: Midiastore (com.rdf112.midiastore)" -ForegroundColor Green
    Write-Host "APK Debug é apenas para testes; não enviar à Google Play." -ForegroundColor Yellow
    Write-Host "==============================================================" -ForegroundColor Cyan
}
finally {
    Pop-Location
}
