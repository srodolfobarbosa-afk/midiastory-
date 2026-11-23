#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "=== 1. Ativando modo 24/7 no Moto G84 (Wake Lock) ==="
termux-wake-lock || true

echo "=== 2. Atualizando Termux e instalando Node.js + Git + OpenSSH ==="
pkg update -y && pkg upgrade -y
pkg install -y nodejs-lts git curl openssh

echo "=== 3. Instalando PM2 (gerenciador 24/7) ==="
npm install -g pm2

echo "=== 4. Instalando Google Antigravity CLI (agy) nativo Android ARM64 ==="
curl -fsSL https://antigravity.google/cli/install.sh | bash

echo "=== 5. Clonando/Atualizando repositorio Midiastore ==="
if [ ! -d "$HOME/midiastory-" ]; then
  git clone https://github.com/srodolfobarbosa-afk/midiastory-.git "$HOME/midiastory-"
fi
cd "$HOME/midiastory-"
git fetch origin
git checkout codex/midiastore-release-audit || true
git pull origin codex/midiastore-release-audit || true

echo ""
echo "======================================================"
echo "PRONTO! Seu Moto G84 agora e seu servidor Linux 24h!"
echo "Para abrir o Antigravity aqui dentro, digite: agy"
echo "======================================================"
