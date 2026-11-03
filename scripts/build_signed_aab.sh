#!/usr/bin/env bash
# Script de homologação e geração de AAB assinado com bloqueio estrito (fail-closed) de certificado
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

EXPECTED_SHA256="${SWEEP_EXPECTED_CERT_SHA256:-AD:3A:7A:DB:D3:F1:4F:FE:20:C9:99:80:B3:3D:FF:D4:1C:AA:39:D2:06:99:08:F0:D1:24:DF:24:91:2C:B9:06}"

printf '\n=== SWEEP (com.rdf112.sweep) — HOMOLOGAÇÃO E AAB RELEASE ===\n'

# 1. Executa testes unitários, lint, build debug e build de testes instrumentados
bash "$ROOT/scripts/verify.sh"

# 2. Sincroniza o JAR do Gradle Wrapper para a versão 8.13 oficial
printf '\n=== SINCRONIZANDO GRADLE WRAPPER 8.13 ===\n'
./gradlew --no-daemon wrapper --gradle-version 8.13 --distribution-type bin

# 3. Verifica se keystore.properties (fornecido localmente fora do ZIP) e o arquivo JKS existem
if [[ ! -f "$ROOT/keystore.properties" ]]; then
  echo 'ERRO BLOQUEANTE: keystore.properties não encontrado na raiz do projeto.' >&2
  echo 'Crie keystore.properties localmente a partir de keystore.properties.example (nunca empacote credenciais no ZIP).' >&2
  exit 2
fi

STORE_FILE="$(sed -n 's/^storeFile=//p' "$ROOT/keystore.properties" | head -n 1)"
if [[ -z "$STORE_FILE" || ! -f "$ROOT/$STORE_FILE" ]]; then
  echo "ERRO BLOQUEANTE: Arquivo JKS ('$STORE_FILE') não encontrado em $ROOT." >&2
  exit 3
fi

# 4. Valida o certificado do JKS ANTES de gerar o AAB
KEY_ALIAS="$(sed -n 's/^keyAlias=//p' "$ROOT/keystore.properties" | head -n 1)"
STORE_PASS="$(sed -n 's/^storePassword=//p' "$ROOT/keystore.properties" | head -n 1)"
JKS_CERT="$(keytool -list -v -keystore "$ROOT/$STORE_FILE" -alias "$KEY_ALIAS" -storepass "$STORE_PASS" 2>/dev/null || true)"
if ! printf '%s' "$JKS_CERT" | grep -qi "$EXPECTED_SHA256"; then
  echo "ERRO BLOQUEANTE (exit 5): O certificado do JKS não corresponde ao SHA-256 esperado ($EXPECTED_SHA256)." >&2
  exit 5
fi

printf '\n=== GERANDO AAB RELEASE ASSINADO (:app:bundleRelease) ===\n'
./gradlew --no-daemon :app:bundleRelease

AAB="$ROOT/app/build/outputs/bundle/release/app-release.aab"
[[ -f "$AAB" ]] || { echo 'ERRO: AAB Release não foi gerado.' >&2; exit 4; }

# 5. Valida o certificado do AAB gerado (falha com código 5 se divergir)
CERT_INFO="$(keytool -printcert -jarfile "$AAB" 2>/dev/null || true)"
printf '%s\n' "$CERT_INFO" | grep -E "Owner:|Issuer:|SHA1:|SHA256:" || true

if ! printf '%s' "$CERT_INFO" | grep -qi "$EXPECTED_SHA256"; then
  echo "ERRO BLOQUEANTE (exit 5): O fingerprint SHA-256 do AAB gerado diverge do certificado esperado ($EXPECTED_SHA256)." >&2
  rm -f "$AAB"
  exit 5
fi

printf '\nOK: Certificado SHA-256 validado com sucesso (%s).\n' "$EXPECTED_SHA256"
ls -lh "$AAB"
sha256sum "$AAB"
