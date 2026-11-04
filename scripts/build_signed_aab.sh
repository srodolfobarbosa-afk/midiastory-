#!/usr/bin/env bash
# Release do Midiastore. Fail-closed: exige aprovação, package/version confirmados
# e a chave de upload legítima. Nunca publica na Google Play.
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

EXPECTED_PACKAGE="com.rdf112.midiastore"
APP_GRADLE="$ROOT/app/build.gradle.kts"

fail() {
  printf 'ERRO BLOQUEANTE: %s\n' "$*" >&2
  exit 1
}

printf '\n=== MIDIastore — RELEASE CONTROLADO ===\n'

# 1. Aprovação do proprietário e identidade confirmada na Play Console.
[[ "${MIDIASTORE_RELEASE_APPROVED:-}" == "YES" ]] || fail \
  "MIDIASTORE_RELEASE_APPROVED deve ser YES em segredo protegido do environment play-release."

CONFIRMED_VERSION_CODE="${MIDIASTORE_CONFIRMED_VERSION_CODE:-}"
[[ "$CONFIRMED_VERSION_CODE" =~ ^[1-9][0-9]*$ ]] || fail \
  "MIDIASTORE_CONFIRMED_VERSION_CODE precisa conter o maior versionCode confirmado na Play Console."

EXPECTED_CERT_SHA256="${MIDIASTORE_EXPECTED_CERT_SHA256:-}"
[[ -n "$EXPECTED_CERT_SHA256" ]] || fail \
  "MIDIASTORE_EXPECTED_CERT_SHA256 não foi configurado. Não use certificado presumido."

SOURCE_PACKAGE="$(sed -nE 's/^[[:space:]]*applicationId[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$APP_GRADLE" | head -n 1)"
SOURCE_VERSION_CODE="$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*([0-9]+).*/\1/p' "$APP_GRADLE" | head -n 1)"

[[ "$SOURCE_PACKAGE" == "$EXPECTED_PACKAGE" ]] || fail \
  "applicationId no source é '$SOURCE_PACKAGE'; esperado '$EXPECTED_PACKAGE'. Nenhum arquivo foi alterado."
[[ "$SOURCE_VERSION_CODE" == "$CONFIRMED_VERSION_CODE" ]] || fail \
  "versionCode no source ($SOURCE_VERSION_CODE) difere do maior versionCode confirmado ($CONFIRMED_VERSION_CODE)."

# 2. Exigir keystore restaurado pelo workflow ou informado explicitamente no ambiente local.
STORE_FILE="${SWEEP_STORE_FILE:-}"
STORE_PASSWORD="${SWEEP_STORE_PASSWORD:-}"
KEY_ALIAS="${SWEEP_KEY_ALIAS:-}"
KEY_PASSWORD="${SWEEP_KEY_PASSWORD:-}"

[[ -n "$STORE_FILE" && -f "$STORE_FILE" ]] || fail \
  "SWEEP_STORE_FILE não aponta para o JKS legítimo."
[[ -n "$STORE_PASSWORD" ]] || fail "SWEEP_STORE_PASSWORD ausente."
[[ -n "$KEY_ALIAS" ]] || fail "SWEEP_KEY_ALIAS ausente."
[[ -n "$KEY_PASSWORD" ]] || fail "SWEEP_KEY_PASSWORD ausente."

# Normaliza fingerprints removendo ':' e espaços, sem imprimir segredos.
normalize_sha256() {
  printf '%s' "$1" | tr -d ':[:space:]' | tr '[:lower:]' '[:upper:]'
}

EXPECTED_CERT_NORMALIZED="$(normalize_sha256 "$EXPECTED_CERT_SHA256")"
[[ "$EXPECTED_CERT_NORMALIZED" =~ ^[A-F0-9]{64}$ ]] || fail \
  "MIDIASTORE_EXPECTED_CERT_SHA256 não é um fingerprint SHA-256 de 64 dígitos hexadecimais."

KEYSTORE_CERT_INFO="$(keytool -list -v -keystore "$STORE_FILE" -alias "$KEY_ALIAS" \
  -storepass "$STORE_PASSWORD" -keypass "$KEY_PASSWORD" 2>/dev/null)" || fail \
  "Não foi possível abrir o JKS com o alias fornecido."

KEYSTORE_CERT_SHA256="$(printf '%s\n' "$KEYSTORE_CERT_INFO" | awk '/SHA256:/{print $2; exit}')"
[[ -n "$KEYSTORE_CERT_SHA256" ]] || fail "Não foi possível obter SHA-256 do certificado do JKS."
[[ "$(normalize_sha256 "$KEYSTORE_CERT_SHA256")" == "$EXPECTED_CERT_NORMALIZED" ]] || fail \
  "O certificado do JKS não corresponde ao fingerprint aprovado na Play Console."

# 3. Gates locais de compilação/teste. O job assinado também depende de verify-debug no CI.
printf '\n=== Executando gates JVM, Lint e Debug ===\n'
./gradlew --no-daemon \
  :domain:test \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest

# 4. Release. O build.gradle.kts lê as mesmas variáveis SWEEP_* sem criar arquivos de senha.
printf '\n=== Gerando APK/AAB Release assinado ===\n'
./gradlew --no-daemon :app:assembleRelease :app:bundleRelease

AAB_SOURCE="$ROOT/app/build/outputs/bundle/release/app-release.aab"
APK_SOURCE="$ROOT/app/build/outputs/apk/release/app-release.apk"
[[ -f "$AAB_SOURCE" ]] || fail "AAB Release não foi gerado."
[[ -f "$APK_SOURCE" ]] || fail "APK Release não foi gerado."

# 5. Confirmar package do APK gerado.
BUILD_TOOLS="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}/build-tools/35.0.0"
AAPT="$BUILD_TOOLS/aapt"
APKSIGNER="$BUILD_TOOLS/apksigner"
[[ -x "$AAPT" ]] || fail "aapt não encontrado em $BUILD_TOOLS."
[[ -x "$APKSIGNER" ]] || fail "apksigner não encontrado em $BUILD_TOOLS."

APK_PACKAGE="$("$AAPT" dump badging "$APK_SOURCE" | sed -n "s/^package: name='\\([^']*\\)'.*/\\1/p" | head -n 1)"
[[ "$APK_PACKAGE" == "$EXPECTED_PACKAGE" ]] || fail \
  "Package do APK Release é '$APK_PACKAGE', esperado '$EXPECTED_PACKAGE'."

# 6. Verificar assinatura do APK e do AAB contra o certificado aprovado.
APK_CERT_INFO="$("$APKSIGNER" verify --verbose --print-certs "$APK_SOURCE")" || fail \
  "apksigner não conseguiu validar o APK Release."
APK_CERT_SHA256="$(printf '%s\n' "$APK_CERT_INFO" | awk -F': ' '/Signer #1 certificate SHA-256 digest:/{print $2; exit}')"
[[ -n "$APK_CERT_SHA256" ]] || fail "Não foi possível obter o fingerprint do APK."
[[ "$(normalize_sha256 "$APK_CERT_SHA256")" == "$EXPECTED_CERT_NORMALIZED" ]] || fail \
  "O certificado do APK não corresponde ao certificado aprovado."

AAB_CERT_INFO="$(keytool -printcert -jarfile "$AAB_SOURCE" 2>/dev/null)" || fail \
  "keytool não conseguiu validar o AAB Release."
AAB_CERT_SHA256="$(printf '%s\n' "$AAB_CERT_INFO" | awk '/SHA256:/{print $2; exit}')"
[[ -n "$AAB_CERT_SHA256" ]] || fail "Não foi possível obter o fingerprint do AAB."
[[ "$(normalize_sha256 "$AAB_CERT_SHA256")" == "$EXPECTED_CERT_NORMALIZED" ]] || fail \
  "O certificado do AAB não corresponde ao certificado aprovado."

# 7. Exportar artefatos para o workflow de CI; não enviar para a Play Store.
for output in app-release.aab app-release.apk app-release.aab.sha256 app-release.apk.sha256; do
  [[ ! -e "$ROOT/$output" ]] || fail "O arquivo de saída já existe: $output. Remova-o conscientemente antes de repetir."
done

cp "$AAB_SOURCE" "$ROOT/app-release.aab"
cp "$APK_SOURCE" "$ROOT/app-release.apk"
(
  cd "$ROOT"
  sha256sum app-release.aab > app-release.aab.sha256
  sha256sum app-release.apk > app-release.apk.sha256
)

printf '\nRelease validado. Package: %s | versionCode: %s\n' "$APK_PACKAGE" "$SOURCE_VERSION_CODE"
printf 'Fingerprint aprovado: %s\n' "$EXPECTED_CERT_SHA256"
ls -lh "$ROOT"/app-release.aab "$ROOT"/app-release.apk "$ROOT"/app-release.aab.sha256 "$ROOT"/app-release.apk.sha256
printf '\nOs arquivos estão apenas como artifacts do workflow. Nenhuma publicação foi realizada.\n'
