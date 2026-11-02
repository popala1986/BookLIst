#!/usr/bin/env bash
# Uruchomienie i aktualizacja BookList na serwerze.
# Uruchom na serwerze:
#   bash <(curl -fsSL https://raw.githubusercontent.com/popala1986/BookLIst/master/deploy/setup-server.sh)
# BookList ma własny kontener z bazą (booklist-db). Z innymi aplikacjami dzieli tylko Caddy,
# któremu skrypt kładzie blok deploy/booklist.caddy do ~/home-app/caddy-sites/.
# Można go uruchamiać wiele razy: gdy ~/booklist/.env już istnieje, tylko pobiera nową wersję i restartuje.
# Jeśli BookList używał wcześniej bazy w kontenerze "postgres", skrypt raz przenosi książki do własnej bazy.
set -euo pipefail

APP_DIR="$HOME/booklist"
# Folder, z którego Caddy wczytuje bloki innych aplikacji (import /etc/caddy/sites/*.caddy).
CADDY_SITES_DIR="${CADDY_SITES_DIR:-$HOME/home-app/caddy-sites}"
RAW="${BOOKLIST_RAW:-https://raw.githubusercontent.com/popala1986/BookLIst/master}"
NEW_DB_URL="jdbc:postgresql://booklist-db:5432/booklist"

env_value() { grep "^$1=" .env | head -1 | cut -d= -f2- | sed -e "s/^'//" -e "s/'$//"; }

wait_for_db() {
  for _ in $(seq 1 40); do
    if docker exec booklist-db pg_isready -U "$DB_USER" -d booklist >/dev/null 2>&1; then return 0; fi
    sleep 3
  done
  echo "Baza booklist-db nie wystartowała. Logi:"; docker logs --tail 40 booklist-db; exit 1
}

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

  umask 077
  cat > .env <<EOF
DOCKER_NETWORK=home-app_default
DB_URL=$NEW_DB_URL
DB_USERNAME=booklist
DB_PASSWORD=$(openssl rand -hex 24)
APP_ADMIN_USERNAME=$ADMIN_USER
APP_ADMIN_PASSWORD='$ADMIN_PASS'
APP_REGISTRATION_ENABLED=true
APP_REMEMBER_ME_KEY=$(openssl rand -hex 32)
GOOGLE_BOOKS_API_KEY=$GOOGLE_KEY
EOF
  echo "Zapisano $APP_DIR/.env"
fi

DB_USER=$(env_value DB_USERNAME)
DB_PASS=$(env_value DB_PASSWORD)

echo "== Pobieram BookList i jego bazę =="
docker compose pull

MIGRATED=false
if [ "$(env_value DB_URL)" != "$NEW_DB_URL" ]; then
  echo "== Przenoszę książki ze wspólnego Postgresa do własnej bazy BookList =="
  BACKUP="$APP_DIR/booklist-przed-przeniesieniem-$(date +%F-%H%M).sql"
  # Gdyby coś poszło nie tak, uruchom z powrotem starą wersję, żeby strona nie zniknęła.
  trap 'echo "Przenoszenie nie powiodło się. Uruchamiam starą wersję BookList (stara baza jest nietknięta)."; docker start booklist >/dev/null 2>&1 || true' ERR
  docker stop booklist >/dev/null 2>&1 || true
  docker exec -e PGPASSWORD="$DB_PASS" postgres \
    pg_dump -U "$DB_USER" -d booklist --no-owner --no-privileges > "$BACKUP"
  echo "Kopia starej bazy: $BACKUP"

  docker compose up -d booklist-db
  wait_for_db
  # Czysta baza na wypadek, gdyby poprzednia próba przerwała się w połowie.
  docker exec booklist-db psql -q -v ON_ERROR_STOP=1 -U "$DB_USER" -d booklist \
    -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
  docker exec -i booklist-db psql -q -v ON_ERROR_STOP=1 -U "$DB_USER" -d booklist < "$BACKUP" >/dev/null
  BOOKS=$(docker exec booklist-db psql -tA -U "$DB_USER" -d booklist -c "SELECT count(*) FROM book" 2>/dev/null || echo 0)
  echo "Przeniesiono książek: $BOOKS"

  sed -i "s|^DB_URL=.*|DB_URL=$NEW_DB_URL|" .env
  trap - ERR
  MIGRATED=true
fi

echo "== Uruchamiam BookList =="
docker compose up -d

echo "== Konfiguracja Caddy (book.homeduty.pl) =="
mkdir -p "$CADDY_SITES_DIR"
curl -fsSL "$RAW/deploy/booklist.caddy" -o "$CADDY_SITES_DIR/booklist.caddy"
if docker exec caddy caddy reload --config /etc/caddy/Caddyfile >/dev/null 2>&1; then
  echo "Caddy przeładowany."
else
  echo "Nie udało się przeładować Caddy. Plik jest gotowy w $CADDY_SITES_DIR."
fi

echo "== Czekam na start aplikacji =="
for _ in $(seq 1 30); do
  if docker logs booklist 2>&1 | grep -q "Started BookListApplication"; then
    echo "BookList działa: https://book.homeduty.pl"
    if [ "$MIGRATED" = true ]; then
      echo
      echo "BookList ma teraz własną bazę (kontener booklist-db)."
      echo "Stara baza w kontenerze postgres została nietknięta. Gdy sprawdzisz, że książki są na miejscu,"
      echo "możesz ją usunąć (administrator Postgresa to POSTGRES_USER z ~/home-app/.env, zwykle homeapp):"
      echo "  docker exec postgres psql -U homeapp -d postgres -c \"DROP DATABASE booklist;\" -c \"DROP USER booklist;\""
    fi
    exit 0
  fi
  sleep 3
done
echo "Aplikacja nie potwierdziła startu w 90 s. Ostatnie logi:"
docker logs --tail 40 booklist
exit 1
