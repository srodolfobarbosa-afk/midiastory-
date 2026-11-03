# Executa testes unitários do domínio e do módulo app
$ErrorActionPreference = "Stop"
Write-Host "=== [3/7] Executando testes unitários (:domain:test e :app:testDebugUnitTest) ===" -ForegroundColor Cyan
cmd /c "call gradlew.bat --no-daemon :domain:test :app:testDebugUnitTest"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Falha nos testes unitários." -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host "Todos os testes unitários foram aprovados com sucesso!" -ForegroundColor Green
