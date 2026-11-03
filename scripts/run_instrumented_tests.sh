#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ ! -x "$ROOT/gradlew" ]]; then
  echo 'ERRO: Gradle Wrapper não encontrado.' >&2
  exit 1
fi
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK" && -f "$ROOT/local.properties" ]]; then
  SDK="$(sed -n 's/^sdk.dir=//p' "$ROOT/local.properties" | head -n 1)"
fi
if [[ -z "$SDK" || ! -d "$SDK/platform-tools" ]]; then
  echo 'ERRO: configure ANDROID_SDK_ROOT para um Android SDK instalado.' >&2
  exit 2
fi
if ! command -v adb >/dev/null 2>&1; then
  if [[ -x "$SDK/platform-tools/adb" ]]; then
    export PATH="$SDK/platform-tools:$PATH"
  else
    echo 'ERRO: adb não encontrado.' >&2
    exit 2
  fi
fi
DEVICES="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
if [[ -z "$DEVICES" ]]; then
  echo 'ERRO: nenhum emulador ou aparelho autorizado está conectado.' >&2
  adb devices -l
  exit 3
fi
printf 'Dispositivos detectados:\n'
adb devices -l
printf '\nExecutando testes instrumentados conectados...\n'
./gradlew --no-daemon :app:connectedDebugAndroidTest
