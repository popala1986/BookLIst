#!/usr/bin/env bash
# Kopia zapasowa bazy BookList. Zapisuje ~/booklist/backups/booklist-RRRR-MM-DD-GGMM.sql.gz
# i zostawia tylko KEEP (domyślnie 14) najnowszych kopii.
# Uruchamiany codziennie przez cron (włącza go deploy/enable-backup.sh), można też ręcznie:
#   ~/booklist/backup.sh
set -euo pipefail

APP_DIR="$HOME/booklist"
BACKUP_DIR="$APP_DIR/backups"
KEEP="${KEEP:-14}"

cd "$APP_DIR"
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

DB_USER=$(grep '^DB_USERNAME=' .env | head -1 | cut -d= -f2-)
FILE="$BACKUP_DIR/booklist-$(date +%F-%H%M).sql.gz"

# Najpierw do pliku tymczasowego, żeby przerwana kopia nie wyglądała na dobrą.
trap 'rm -f "$FILE.tmp"' EXIT
docker exec booklist-db pg_dump -U "$DB_USER" -d booklist --no-owner --no-privileges | gzip > "$FILE.tmp"
mv "$FILE.tmp" "$FILE"
chmod 600 "$FILE"

# Usuń najstarsze kopie, zostaw KEEP najnowszych.
ls -1t "$BACKUP_DIR"/booklist-*.sql.gz | tail -n +"$((KEEP + 1))" | xargs -r rm -f --

echo "$(date '+%F %T') Kopia: $FILE ($(du -h "$FILE" | cut -f1)), kopii na serwerze: $(ls -1 "$BACKUP_DIR"/booklist-*.sql.gz | wc -l)"
