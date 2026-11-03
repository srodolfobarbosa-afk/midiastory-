#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

printf '\n=== SWEEP v0.3 — TESTES, LINT E BUILD DEBUG ===\n'
printf 'Projeto: %s\n' "$ROOT"

if ! command -v java >/dev/null 2>&1; then
  echo 'ERRO: Java não encontrado. Instale JDK 17 ou superior.' >&2
  exit 1
fi
JAVA_SPEC="$(java -XshowSettings:properties -version 2>&1 | awk -F'= ' '/java.specification.version/ {print $2; exit}')"
JAVA_MAJOR="${JAVA_SPEC#1.}"
if [[ ! "$JAVA_MAJOR" =~ ^[0-9]+$ ]] || (( JAVA_MAJOR < 17 )); then
  echo "ERRO: JDK 17+ é obrigatório. Encontrado: ${JAVA_SPEC:-desconhecido}" >&2
  exit 1
fi
java -version

if [[ ! -x "$ROOT/gradlew" || ! -f "$ROOT/gradle/wrapper/gradle-wrapper.jar" ]]; then
  echo 'ERRO: Gradle Wrapper ausente ou incompleto.' >&2
  exit 1
fi

AVAILABLE_KB="$(df -Pk "$ROOT" | awk 'NR==2 {print $4}')"
AVAILABLE_GB="$(awk -v kb="$AVAILABLE_KB" 'BEGIN {printf "%.2f", kb/1024/1024}')"
printf 'Espaço disponível no volume: %s GB\n' "$AVAILABLE_GB"
if (( AVAILABLE_KB < 4194304 )); then
  echo 'ERRO: menos de 4 GB livres. Nenhum arquivo foi apagado; libere espaço antes do build.' >&2
  exit 2
fi

SDK_PATH="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK_PATH" && -f "$ROOT/local.properties" ]]; then
  SDK_PATH="$(sed -n 's/^sdk.dir=//p' "$ROOT/local.properties" | head -n 1)"
fi
if [[ -z "$SDK_PATH" ]]; then
  for CANDIDATE in "$HOME/Android/Sdk" "$HOME/android-sdk" "/usr/local/lib/android/sdk"; do
    if [[ -d "$CANDIDATE/platforms/android-36" ]]; then
      SDK_PATH="$CANDIDATE"
      break
    fi
  done
fi
if [[ -z "$SDK_PATH" || ! -d "$SDK_PATH/platforms/android-36" ]]; then
  echo 'ERRO: Android SDK Platform 36 não localizado. Configure ANDROID_SDK_ROOT.' >&2
  exit 3
fi
if [[ ! -d "$SDK_PATH/build-tools/35.0.0" ]]; then
  echo 'ERRO: Build Tools 35.0.0 não localizado. Instale com sdkmanager "build-tools;35.0.0".' >&2
  exit 3
fi
if [[ ! -f "$ROOT/local.properties" ]]; then
  printf 'sdk.dir=%s\n' "$SDK_PATH" > "$ROOT/local.properties"
fi

printf '\n=== SUÍTE GRADLE ===\n'
./gradlew --no-daemon clean \
  :domain:test \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest

APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
TEST_APK="$ROOT/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
[[ -f "$APK" ]] || { echo 'ERRO: APK Debug não foi gerado.' >&2; exit 4; }
[[ -f "$TEST_APK" ]] || { echo 'ERRO: APK instrumentado não foi gerado.' >&2; exit 4; }

printf '\n=== RESULTADO ===\n'
ls -lh "$APK" "$TEST_APK"
echo 'OK: APK Debug e APK de instrumentação gerados. Ainda é obrigatório executar connectedDebugAndroidTest em emulador/aparelho.'
