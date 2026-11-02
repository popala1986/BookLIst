#!/usr/bin/env bash
# Włącza automatyczne wdrażanie BookList po merge do master.
# Uruchom na serwerze:
#   bash <(curl -fsSL https://raw.githubusercontent.com/popala1986/BookLIst/master/deploy/enable-auto-deploy.sh)
# Skrypt:
#  1. zapisuje ~/booklist/update.sh (pobiera nową wersję i restartuje aplikację),
#  2. tworzy osobny klucz SSH, który może uruchomić TYLKO ten plik (bez dostępu do konsoli),
#  3. wypisuje trzy wartości do wklejenia w GitHub → Settings → Secrets and variables → Actions.
set -euo pipefail

APP_DIR="$HOME/booklist"
RAW="${BOOKLIST_RAW:-https://raw.githubusercontent.com/popala1986/BookLIst/master}"
KEY="$HOME/.ssh/booklist_deploy"
HOST="${DEPLOY_HOST:-homeduty.pl}"

if [ ! -f "$APP_DIR/.env" ]; then
  echo "Najpierw uruchom BookList skryptem setup-server.sh."; exit 1
fi

curl -fsSL "$RAW/deploy/update.sh" -o "$APP_DIR/update.sh"
chmod 700 "$APP_DIR/update.sh"
echo "Zapisano $APP_DIR/update.sh"

mkdir -p "$HOME/.ssh" && chmod 700 "$HOME/.ssh"
if [ ! -f "$KEY" ]; then
  ssh-keygen -q -t ed25519 -N "" -C "booklist-deploy" -f "$KEY"
fi
touch "$HOME/.ssh/authorized_keys" && chmod 600 "$HOME/.ssh/authorized_keys"
# Usuń poprzedni wpis tego klucza i dodaj go z ograniczeniem do jednego polecenia.
grep -v "booklist-deploy" "$HOME/.ssh/authorized_keys" > "$HOME/.ssh/authorized_keys.tmp" || true
echo "restrict,command=\"$APP_DIR/update.sh\" $(cat "$KEY.pub")" >> "$HOME/.ssh/authorized_keys.tmp"
mv "$HOME/.ssh/authorized_keys.tmp" "$HOME/.ssh/authorized_keys"
chmod 600 "$HOME/.ssh/authorized_keys"
echo "Klucz dodany do ~/.ssh/authorized_keys (może uruchomić tylko update.sh)."

HOST_KEY=$(awk '{print $1" "$2}' "${HOST_KEY_FILE:-/etc/ssh/ssh_host_ed25519_key.pub}")

cat <<INFO

================= Sekrety do GitHuba =================
Wejdź na https://github.com/popala1986/BookLIst/settings/secrets/actions
i kliknij "New repository secret" trzy razy:

1) Name: DEPLOY_TARGET
   Secret:
$(whoami)@$HOST

2) Name: DEPLOY_KNOWN_HOSTS
   Secret:
$HOST $HOST_KEY

3) Name: DEPLOY_SSH_KEY
   Secret (wszystkie linijki, razem z BEGIN i END):
$(cat "$KEY")
=====================================================
INFO
