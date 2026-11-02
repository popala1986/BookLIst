# Wdrożenie BookList na serwer

Ta instrukcja zakłada serwer, na którym już działają Docker, Caddy (porty 80/443) i PostgreSQL w kontenerze `postgres`. BookList dołącza do tej samej sieci Dockera, korzysta z istniejącego Postgresa (osobna baza i użytkownik), a Caddy kieruje do niego ruch z nowej domeny i sam wystawia certyfikat HTTPS.

## Jak to działa

- Każdy push na `master` uruchamia GitHub Actions: testy, budowę obrazu i publikację do `ghcr.io/popala1986/booklist:latest`.
- Na serwerze `docker compose pull && docker compose up -d` pobiera nową wersję.
- Aplikacja startuje z profilem `prod` (`application-prod.properties`), a hasła bierze ze zmiennych w pliku `.env`.

## 1. Domena i DNS

Nie trzeba kupować nowej domeny: BookList może działać na subdomenie domeny, którą już masz, np. `book.homeduty.pl`. W panelu OVH (strefa DNS `homeduty.pl`) dodaj rekord:

| Typ | Nazwa | Wartość |
|-----|-------|---------|
| A   | `book` | to samo IPv4, na które wskazuje `homeduty.pl` |

Jeśli `homeduty.pl` ma rekord `AAAA`, dodaj też `AAAA` dla `book`. Jeśli wolisz osobną domenę, kup ją i ustaw rekord `A` tak samo. Propagacja zwykle trwa od kilku minut do godziny. Sprawdzisz ją poleceniem `dig +short book.homeduty.pl`.

## 2. Baza danych w istniejącym Postgresie

Administrator Postgresa w home-app to `homeapp` (zmienna `POSTGRES_USER` w `~/home-app/.env`). Wygeneruj hasło (`openssl rand -hex 24`) i załóż bazę:

```bash
docker exec -it postgres psql -U homeapp -d postgres -c "CREATE USER booklist WITH PASSWORD 'WYGENEROWANE_HASLO';"
docker exec -it postgres psql -U homeapp -d postgres -c "CREATE DATABASE booklist OWNER booklist;"
```

Tabele utworzy aplikacja przy pierwszym starcie.

## 3. Sieć Dockera

Caddy i Postgres z home-app są w sieci `home-app_default`. BookList dołącza do niej (to domyślna wartość `DOCKER_NETWORK` w `.env.example`), więc widzi je pod nazwami `caddy` i `postgres`.

## 4. Uruchomienie aplikacji

Na serwerze:

```bash
mkdir -p ~/booklist && cd ~/booklist
curl -fsSLO https://raw.githubusercontent.com/popala1986/BookLIst/master/docker-compose.yml
curl -fsSL  https://raw.githubusercontent.com/popala1986/BookLIst/master/.env.example -o .env
nano .env        # uzupełnij hasła i DOCKER_NETWORK
chmod 600 .env
```

Obraz w GHCR jest domyślnie prywatny. Jeśli serwer pobiera już obrazy `home-app-*` z GHCR, logowanie jest gotowe. Jeśli nie, zaloguj się tokenem z uprawnieniem `read:packages`:

```bash
echo <TOKEN> | docker login ghcr.io -u popala1986 --password-stdin
```

Start:

```bash
docker compose pull
docker compose up -d
docker logs -f booklist     # poczekaj na "Started BookListApplication"
```

## 5. Caddy

Caddy z home-app czyta `infra/caddy/Caddyfile.server` z repozytorium `java-kafka-home-app`. Na jego końcu dopisz blok:

```caddyfile
book.{$DOMAIN} {
	encode gzip
	reverse_proxy booklist:8080
}
```

Po zmergowaniu do `main` CD w home-app uruchomi `deploy.sh`, który sam utworzy Caddy od nowa, gdy Caddyfile się zmieni. Ręcznie na serwerze:

```bash
cd ~/home-app && git pull --ff-only && docker compose up -d --force-recreate caddy
```

Caddy sam pobierze certyfikat Let's Encrypt, gdy rekord DNS już działa. Wejdź na `https://book.homeduty.pl` i zaloguj się danymi z `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD`.

## Aktualizacja

Po zmergowaniu zmian na `master` i zakończeniu GitHub Actions:

```bash
cd ~/booklist && docker compose pull && docker compose up -d
```

## Kopia zapasowa

```bash
docker exec postgres pg_dump -U booklist booklist > booklist-$(date +%F).sql
```

## Zasoby

Kontener ma limit 512 MB RAM (`mem_limit` w `docker-compose.yml`), a Java używa do 75% tego limitu. Przy obecnym obciążeniu serwera to bezpieczna wartość.

## Lokalnie (bez Dockera)

Skopiuj `src/main/resources/application-example.properties` do `application.properties`, uzupełnij dane MySQL i login, a potem `./mvnw spring-boot:run`.
