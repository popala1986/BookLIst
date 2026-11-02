#!/usr/bin/env bash
# Pierwsze uruchomienie BookList na serwerze z home-app (Caddy + Postgres w sieci home-app_default).
# Uruchom na serwerze:
#   bash <(curl -fsSL https://raw.githubusercontent.com/popala1986/BookLIst/master/deploy/setup-server.sh)
# Skrypt: zakłada bazę i użytkownika w Postgresie, generuje hasła, tworzy ~/booklist/.env i uruchamia aplikację.
# Można go uruchomić ponownie: gdy ~/booklist/.env już istnieje, tylko pobiera nową wersję i restartuje.
set -euo pipefail

APP_DIR="$HOME/booklist"
HOME_APP_ENV="$HOME/home-app/.env"
RAW="https://raw.githubusercontent.com/popala1986/BookLIst/master"

mkdir -p "$APP_DIR"
cd "$APP_DIR"
curl -fsSLO "$RAW/docker-compose.yml"

if [ ! -f .env ]; then
  echo "== Konto administratora BookList =="
  read -rp "Login administratora: " ADMIN_USER
  while true; do
    read -rsp "Hasło administratora (min. 8 znaków): " ADMIN_PASS; echo
    read -rsp "Powtórz hasło: " ADMIN_PASS2; echo
    if [ "${#ADMIN_PASS}" -lt 8 ]; then echo "Za krótkie, spróbuj jeszcze raz."; continue; fi
    if [ "$ADMIN_PASS" != "$ADMIN_PASS2" ]; then echo "Hasła się różnią, spróbuj jeszcze raz."; continue; fi
    if [[ "$ADMIN_PASS" == *"'"* ]]; then echo "Hasło nie może zawierać apostrofu ('), wybierz inne."; continue; fi
    break
  done
  read -rp "Klucz Google Books API (Enter, żeby pominąć): " GOOGLE_KEY

  PG_USER=homeapp
  if [ -f "$HOME_APP_ENV" ] && grep -q '^POSTGRES_USER=' "$HOME_APP_ENV"; then
    PG_USER=$(grep '^POSTGRES_USER=' "$HOME_APP_ENV" | cut -d= -f2 | tr -d '"'"'")
  fi

  DB_PASS=$(openssl rand -hex 24)
  echo "== Zakładam bazę booklist w Postgresie (administrator: $PG_USER) =="
  if docker exec postgres psql -U "$PG_USER" -d postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='booklist'" | grep -q 1; then
    echo "Użytkownik booklist już istnieje: ustawiam mu nowe hasło."
    docker exec postgres psql -v ON_ERROR_STOP=1 -U "$PG_USER" -d postgres -c "ALTER USER booklist WITH PASSWORD '$DB_PASS';"
  else
    docker exec postgres psql -v ON_ERROR_STOP=1 -U "$PG_USER" -d postgres -c "CREATE USER booklist WITH PASSWORD '$DB_PASS';"
  fi
  if ! docker exec postgres psql -U "$PG_USER" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='booklist'" | grep -q 1; then
    docker exec postgres psql -v ON_ERROR_STOP=1 -U "$PG_USER" -d postgres -c "CREATE DATABASE booklist OWNER booklist;"
  fi

  umask 077
  cat > .env <<EOF
DOCKER_NETWORK=home-app_default
DB_URL=jdbc:postgresql://postgres:5432/booklist
DB_USERNAME=booklist
DB_PASSWORD=$DB_PASS
APP_ADMIN_USERNAME=$ADMIN_USER
APP_ADMIN_PASSWORD='$ADMIN_PASS'
APP_REGISTRATION_ENABLED=true
APP_REMEMBER_ME_KEY=$(openssl rand -hex 32)
GOOGLE_BOOKS_API_KEY=$GOOGLE_KEY
EOF
  echo "Zapisano $APP_DIR/.env"
fi

echo "== Pobieram i uruchamiam BookList =="
docker compose pull
docker compose up -d

echo "== Czekam na start aplikacji =="
for _ in $(seq 1 30); do
  if docker logs booklist 2>&1 | grep -q "Started BookListApplication"; then
    echo "BookList działa. Po dodaniu bloku do Caddyfile otwórz https://book.homeduty.pl"
    exit 0
  fi
  sleep 3
done
echo "Aplikacja nie potwierdziła startu w 90 s. Ostatnie logi:"
docker logs --tail 40 booklist
exit 1
