#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ -n "${SWEEP_AAPT2_OVERRIDE:-}" ]]; then
  if [[ ! -x "$SWEEP_AAPT2_OVERRIDE" || "${SWEEP_AAPT2_OVERRIDE##*/}" != "aapt2" ]]; then
    echo "ERRO: SWEEP_AAPT2_OVERRIDE deve apontar para um executável terminado em aapt2." >&2
    exit 1
  fi
  "$SWEEP_AAPT2_OVERRIDE" version >/dev/null
fi

exec bash "$ROOT/scripts/prepare_and_test.sh" "$@"
