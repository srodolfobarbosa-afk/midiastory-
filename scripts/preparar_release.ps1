# Gera os pacotes AAB Release assinados (versionCode = 13 / 2.1.0)
$ErrorActionPreference = "Stop"
Write-Host "=== [6/7] Gerando AAB Release assinado (versionCode = 13) ===" -ForegroundColor Cyan

if ((Test-Path "app\sweep-upload.jks") -and -not (Test-Path "sweep-upload.jks")) {
    Copy-Item "app\sweep-upload.jks" "sweep-upload.jks" -Force
}

# 1. Gera AAB oficial com applicationId = com.rdf112.sweep
(Get-Content "app\build.gradle.kts") -replace 'applicationId\s*=\s*".*?"', 'applicationId = "com.rdf112.sweep"' | Set-Content "app\build.gradle.kts"
cmd /c "call gradlew.bat --no-daemon :app:bundleRelease"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Copy-Item "app\build\outputs\bundle\release\app-release.aab" "$env:USERPROFILE\Downloads\SWEEP_2.1.0_VC13_OFICIAL.aab" -Force

# 2. Gera também AAB com applicationId = com.rdf112.midiastore para o segundo cadastro do Play Console
(Get-Content "app\build.gradle.kts") -replace 'applicationId\s*=\s*".*?"', 'applicationId = "com.rdf112.midiastore"' | Set-Content "app\build.gradle.kts"
cmd /c "call gradlew.bat --no-daemon :app:bundleRelease"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Copy-Item "app\build\outputs\bundle\release\app-release.aab" "$env:USERPROFILE\Downloads\MIDIASTORE_2.1.0_VC13_OFICIAL.aab" -Force

# Restaura com.rdf112.sweep como padrão no código-fonte
(Get-Content "app\build.gradle.kts") -replace 'applicationId\s*=\s*".*?"', 'applicationId = "com.rdf112.sweep"' | Set-Content "app\build.gradle.kts"

Write-Host "AABs gerados na pasta Downloads:" -ForegroundColor Green
Write-Host "  1) $env:USERPROFILE\Downloads\SWEEP_2.1.0_VC13_OFICIAL.aab (para o app com.rdf112.sweep)" -ForegroundColor Green
Write-Host "  2) $env:USERPROFILE\Downloads\MIDIASTORE_2.1.0_VC13_OFICIAL.aab (para o app com.rdf112.midiastore)" -ForegroundColor Green
