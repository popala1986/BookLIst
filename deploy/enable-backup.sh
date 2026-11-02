#!/usr/bin/env bash
# Włącza codzienną kopię zapasową bazy BookList (o 3:30 w nocy, 14 ostatnich dni).
# Uruchom na serwerze:
#   bash <(curl -fsSL https://raw.githubusercontent.com/popala1986/BookLIst/master/deploy/enable-backup.sh)
# Skrypt:
#  1. zapisuje ~/booklist/backup.sh (robi jedną kopię i usuwa najstarsze),
#  2. dodaje go do crona (harmonogramu zadań) Twojego użytkownika; uruchomiony drugi raz nie dubluje wpisu,
#  3. od razu robi pierwszą kopię, żeby było widać, że działa.
set -euo pipefail

APP_DIR="$HOME/booklist"
RAW="${BOOKLIST_RAW:-https://raw.githubusercontent.com/popala1986/BookLIst/master}"
SCHEDULE="${BACKUP_SCHEDULE:-30 3 * * *}"

if [ ! -f "$APP_DIR/.env" ]; then
  echo "Najpierw uruchom BookList skryptem setup-server.sh."; exit 1
fi
if ! command -v crontab >/dev/null 2>&1; then
  echo "Brak programu cron. Zainstaluj go: sudo apt-get install -y cron, i uruchom skrypt jeszcze raz."; exit 1
fi

curl -fsSL "$RAW/deploy/backup.sh" -o "$APP_DIR/backup.sh"
chmod 700 "$APP_DIR/backup.sh"
mkdir -p "$APP_DIR/backups" && chmod 700 "$APP_DIR/backups"
echo "Zapisano $APP_DIR/backup.sh"

# Zastąp poprzedni wpis BookList w cronie (jeśli był) nowym; inne wpisy zostają bez zmian.
CRON_LINE="$SCHEDULE $APP_DIR/backup.sh >> $APP_DIR/backups/backup.log 2>&1"
{ crontab -l 2>/dev/null | grep -v "booklist/backup.sh" || true; echo "$CRON_LINE"; } | crontab -
echo "Cron: $CRON_LINE"

echo "== Pierwsza kopia =="
"$APP_DIR/backup.sh"

cat <<INFO

Gotowe. Kopia robi się codziennie o 3:30 (czas serwera), na serwerze zostaje 14 ostatnich.
Lista kopii:   ls -lh $APP_DIR/backups
Dziennik:      cat $APP_DIR/backups/backup.log
Przywracanie:  opisane w DEPLOY.md, sekcja "Kopia zapasowa".
INFO
