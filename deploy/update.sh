#!/usr/bin/env bash
# Pobiera najnowszą wersję BookList i restartuje aplikację.
# Uruchamiany automatycznie przez GitHub Actions po każdym merge do master (przez klucz SSH,
# który może uruchomić tylko ten plik). Można go też uruchomić ręcznie: ~/booklist/update.sh
set -euo pipefail

cd "$HOME/booklist"
curl -fsSLO https://raw.githubusercontent.com/popala1986/BookLIst/master/docker-compose.yml
docker compose pull -q
docker compose up -d

for _ in $(seq 1 30); do
  if docker logs booklist 2>&1 | grep -q "Started BookListApplication"; then
    echo "BookList działa (wersja $(docker inspect -f '{{.Image}}' booklist | cut -c8-19))."
    docker image prune -f >/dev/null
    exit 0
  fi
  sleep 3
done
echo "BookList nie wystartował w 90 s. Ostatnie logi:"
docker logs --tail 40 booklist
exit 1
