# Executa testes, Lint e builds Debug do Midiastore.
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$gradle = Join-Path $projectRoot "gradlew.bat"
Write-Host "=== Testes, Lint e builds Debug ===" -ForegroundColor Cyan
Push-Location $projectRoot
try {
    $tasks = @(
        ":domain:test",
        ":app:testDebugUnitTest",
        ":app:lintDebug",
        ":app:assembleDebug",
        ":app:assembleDebugAndroidTest"
    )
    & $gradle --no-daemon @tasks
    if ($LASTEXITCODE -ne 0) {
        throw "Gate local de testes/build falhou (exit code $LASTEXITCODE)."
    }
    Write-Host "Testes JVM, Lint e APKs Debug/AndroidTest passaram." -ForegroundColor Green

    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if ($adb) {
        $devices = (& adb devices) | Select-Object -Skip 1 | Where-Object { $_ -match '\sdevice$' }
        if ($devices) {
            Write-Host "Dispositivo autorizado detectado. Executando instrumentação..." -ForegroundColor Cyan
            & $gradle --no-daemon :app:connectedDebugAndroidTest
            if ($LASTEXITCODE -ne 0) {
                throw "Testes instrumentados conectados falharam (exit code $LASTEXITCODE)."
            }
        } else {
            Write-Host "Nenhum dispositivo/emulador autorizado. Testes instrumentados foram pulados, não aprovados." -ForegroundColor Yellow
        }
    } else {
        Write-Host "ADB não encontrado. Testes instrumentados foram pulados, não aprovados." -ForegroundColor Yellow
    }
}
finally {
    Pop-Location
}
