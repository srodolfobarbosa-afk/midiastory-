#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

required_vars=(SWEEP_STORE_FILE SWEEP_STORE_PASSWORD SWEEP_KEY_ALIAS SWEEP_KEY_PASSWORD SWEEP_EXPECTED_CERT_SHA256)
for name in "${required_vars[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    printf 'ERRO: configure %s como secret/variável protegida do proprietário.\n' "$name" >&2
    exit 2
  fi
done

if [[ -z "${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}" ]]; then
  if [[ -f "$ROOT/local.properties" ]]; then
    sdk_dir="$(sed -n 's/^sdk.dir=//p' "$ROOT/local.properties" | head -n 1)"
    export ANDROID_SDK_ROOT="${sdk_dir//\\:/:}"
  elif [[ -d "$HOME/Android/Sdk" ]]; then
    export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
  else
    echo 'ERRO: configure ANDROID_SDK_ROOT para um SDK Android instalado.' >&2
    exit 3
  fi
fi
export ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo 'ERRO: configure JAVA_HOME para JDK 17.' >&2
  exit 3
fi
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

if [[ "${SWEEP_STORE_FILE}" = /* ]]; then
  store_file="$SWEEP_STORE_FILE"
else
  store_file="$ROOT/$SWEEP_STORE_FILE"
fi
if [[ ! -f "$store_file" ]]; then
  echo 'ERRO: SWEEP_STORE_FILE não aponta para o keystore legítimo do proprietário.' >&2
  exit 4
fi
export SWEEP_STORE_FILE="$store_file"

expected_sha="$(printf '%s' "$SWEEP_EXPECTED_CERT_SHA256" | tr -d '[:space:]:' | tr '[:upper:]' '[:lower:]')"
if [[ ! "$expected_sha" =~ ^[0-9a-f]{64}$ ]]; then
  echo 'ERRO: SWEEP_EXPECTED_CERT_SHA256 deve conter o fingerprint SHA-256 legítimo (64 hex chars).' >&2
  exit 4
fi

keystore_sha="$(keytool -list -v -keystore "$SWEEP_STORE_FILE" -alias "$SWEEP_KEY_ALIAS" -storepass:env SWEEP_STORE_PASSWORD 2>/dev/null | awk -F': ' '/SHA256:/{gsub(":", "", $2); print tolower($2); exit}')"
if [[ "$keystore_sha" != "$expected_sha" ]]; then
  echo 'ERRO: o certificado configurado não corresponde ao fingerprint esperado; build cancelado.' >&2
  exit 5
fi

output="$ROOT/app-release.aab"
checksum_file="$ROOT/app-release.aab.sha256"
apk_output="$ROOT/app-release.apk"
apk_checksum_file="$ROOT/app-release.apk.sha256"
if [[ -e "$output" || -e "$checksum_file" || -e "$apk_output" || -e "$apk_checksum_file" ]]; then
  echo 'ERRO: um artefato/checksum Release já existe na raiz; mova o anterior antes de substituir.' >&2
  exit 6
fi

printf '\n=== TESTES, LINT E BUILD ASSINADO ===\n'
./gradlew --no-daemon clean \
  :domain:test \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest \
  :app:assembleRelease \
  :app:bundleRelease

aab_source="$ROOT/app/build/outputs/bundle/release/app-release.aab"
apk_source="$ROOT/app/build/outputs/apk/release/app-release.apk"
if [[ ! -s "$aab_source" || ! -s "$apk_source" ]]; then
  echo 'ERRO: Gradle terminou sem gerar APK e AAB Release não vazios.' >&2
  exit 7
fi

actual_sha="$(keytool -printcert -jarfile "$aab_source" 2>/dev/null | awk -F': ' '/SHA256:/{gsub(":", "", $2); print tolower($2); exit}')"
apk_sha="$(keytool -printcert -jarfile "$apk_source" 2>/dev/null | awk -F': ' '/SHA256:/{gsub(":", "", $2); print tolower($2); exit}')"
if [[ "$actual_sha" != "$expected_sha" || "$apk_sha" != "$expected_sha" ]]; then
  echo 'ERRO: o certificado incorporado no AAB diverge do fingerprint esperado; artefato não exportado.' >&2
  exit 5
fi
jarsigner -verify "$aab_source" >/dev/null
jarsigner -verify "$apk_source" >/dev/null

temporary_aab="$(mktemp "$ROOT/.app-release.aab.XXXXXX")"
temporary_apk="$(mktemp "$ROOT/.app-release.apk.XXXXXX")"
trap 'rm -f "$temporary_aab" "$temporary_apk"' EXIT
cp "$aab_source" "$temporary_aab"
cp "$apk_source" "$temporary_apk"
mv "$temporary_aab" "$output"
mv "$temporary_apk" "$apk_output"
trap - EXIT
sha256sum "$output" > "$checksum_file"
sha256sum "$apk_output" > "$apk_checksum_file"

printf 'AAB assinado validado e exportado: %s\n' "$output"
cat "$checksum_file"
printf 'APK assinado validado e exportado: %s\n' "$apk_output"
cat "$apk_checksum_file"
