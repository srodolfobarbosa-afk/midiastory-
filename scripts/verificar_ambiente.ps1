# Verifica pré-requisitos para compilar o Midiastore no Windows.
$ErrorActionPreference = "Stop"
Write-Host "=== Verificando ambiente Windows do Midiastore ===" -ForegroundColor Cyan

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    throw "Java/JDK 17 não encontrado no PATH."
}
$javaVersionText = (& java -version 2>&1 | Out-String)
if ($javaVersionText -notmatch 'version\s+"17([\.]|")') {
    Write-Host $javaVersionText -ForegroundColor Yellow
    throw "O build requer JDK 17. A versão detectada não é 17."
}
Write-Host "JDK 17 encontrado: $($javaCmd.Source)" -ForegroundColor Green

$sdk = $env:ANDROID_SDK_ROOT
if ([string]::IsNullOrWhiteSpace($sdk)) { $sdk = $env:ANDROID_HOME }
if ([string]::IsNullOrWhiteSpace($sdk) -and (Test-Path ".\local.properties")) {
    $line = Get-Content ".\local.properties" | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
    if ($line) {
        $sdk = $line.Substring($line.IndexOf('=') + 1)
        $sdk = $sdk.Replace('\:', ':').Replace('\\', '\')
    }
}
if ([string]::IsNullOrWhiteSpace($sdk) -or -not (Test-Path (Join-Path $sdk "platform-tools"))) {
    Write-Host "AVISO: Android SDK não detectado. Configure ANDROID_SDK_ROOT ou local.properties." -ForegroundColor Yellow
} else {
    Write-Host "Android SDK: $sdk" -ForegroundColor Green
}

Write-Host "Chave de upload não é necessária para testes Debug." -ForegroundColor Green
Write-Host "Release/AAB permanece bloqueado nesta rotina local." -ForegroundColor Yellow
