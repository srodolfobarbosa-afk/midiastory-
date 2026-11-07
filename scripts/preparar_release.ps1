# Release/AAB é intencionalmente bloqueado neste script local.
# O script antigo alterava applicationId, gerava dois pacotes e restaurava o package errado.
# A geração assinada deve ocorrer somente no workflow protegido play-release após os gates P0.
$ErrorActionPreference = "Stop"
Write-Host "=== RELEASE BLOQUEADO — SOMENTE APK DEBUG NESTA ROTINA ===" -ForegroundColor Yellow
Write-Host "Package correto: com.rdf112.midiastore" -ForegroundColor Cyan
Write-Host "Nenhum applicationId, versionCode, keystore ou arquivo de build será alterado." -ForegroundColor Green
Write-Host "Não gerar AAB antes de confirmar versionCode publicado, chave de upload, Billing e testes físicos." -ForegroundColor Yellow
Write-Host "Quando aprovado, use GitHub Actions > Android > Run workflow no ambiente protegido play-release." -ForegroundColor Cyan
return
