#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

BRANCH="feature/midiastore-2027-release-consolidation"
CURRENT="$(git branch --show-current)"

if [[ "$CURRENT" != "$BRANCH" ]]; then
  echo "ERRO: branch incorreta: $CURRENT"
  exit 1
fi

if ! git diff --cached --quiet; then
  echo "ERRO: existem arquivos staged manualmente."
  echo "Revise o índice antes de executar novamente."
  exit 1
fi

echo "===== SALVANDO MIDIastore ====="

git add -A

BAD_PATHS="$(git diff --cached --name-only | grep -Ei '(^|/)(\.env($|\.)|local\.properties$|keystore\.properties$|[^/]*\.(jks|keystore|p12|pfx|pem|key|apk|aab)$|[^/]*service-account[^/]*\.json$|[^/]*credentials[^/]*\.json$|build/|\.gradle/)' || true)"

if [[ -n "$BAD_PATHS" ]]; then
  echo "ARQUIVOS SENSÍVEIS OU BINÁRIOS DETECTADOS:"
  echo "$BAD_PATHS"
  git reset
  echo "Nenhuma alteração foi enviada. Revise os arquivos."
  exit 1
fi

SECRET_HITS="$(git grep --cached -I -l -E '(-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----|AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{20,}|AIza[0-9A-Za-z_-]{30,})' -- . 2>/dev/null || true)"

if [[ -n "$SECRET_HITS" ]]; then
  echo "POSSÍVEIS SEGREDOS DETECTADOS:"
  echo "$SECRET_HITS"
  git reset
  echo "Nada foi enviado. Investigue os arquivos."
  exit 1
fi

git diff --cached --check

if ! git diff --cached --quiet; then
  MESSAGE="${1:-chore: salvar progresso Midiastore}"
  git commit -m "$MESSAGE"
else
  echo "Nenhuma alteração nova para commit."
fi

echo "===== ENVIANDO PARA O GITHUB ====="

git push -u origin "$BRANCH"

echo "===== PONTO DE RESTAURACAO CONFIRMADO ====="
git status --short --branch
git log -1 --oneline
