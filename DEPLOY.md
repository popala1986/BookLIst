# Wdrożenie BookList na serwer

Ta instrukcja zakłada serwer, na którym już działają Docker i Caddy (porty 80/443). BookList ma własną bazę PostgreSQL w osobnym kontenerze `booklist-db`, więc nie zależy od baz innych aplikacji. Jedyne, co dzieli z innymi, to Caddy: kieruje ruch z domeny do BookList i sam wystawia certyfikat HTTPS.

## Jak to działa

- Każdy push na `master` uruchamia GitHub Actions: testy, budowę obrazu i publikację do `ghcr.io/popala1986/booklist:latest`.
- Na serwerze `docker compose pull && docker compose up -d` pobiera nową wersję.
- Aplikacja startuje z profilem `prod` (`application-prod.properties`), a hasła bierze ze zmiennych w pliku `.env`.

## Szybki start

Po zmergowaniu do `master` i zakończeniu GitHub Actions wystarczą trzy rzeczy:

1. Rekord DNS `book` w OVH (punkt 1 niżej).
2. Na serwerze jedno polecenie, które generuje hasła, tworzy `.env` i uruchamia aplikację razem z jej bazą (zapyta o login i hasło administratora):
   ```bash
   bash <(curl -fsSL https://raw.githubusercontent.com/popala1986/BookLIst/master/deploy/setup-server.sh)
   ```
3. Nic więcej: skrypt sam dokłada blok Caddy (punkt 5 niżej).

Poniżej te same kroki opisane ręcznie.

## 1. Domena i DNS

Nie trzeba kupować nowej domeny: BookList może działać na subdomenie domeny, którą już masz, np. `book.homeduty.pl`. W panelu OVH (strefa DNS `homeduty.pl`) dodaj rekord:

| Typ | Nazwa | Wartość |
|-----|-------|---------|
| A   | `book` | to samo IPv4, na które wskazuje `homeduty.pl` |

Jeśli `homeduty.pl` ma rekord `AAAA`, dodaj też `AAAA` dla `book`. Jeśli wolisz osobną domenę, kup ją i ustaw rekord `A` tak samo. Propagacja zwykle trwa od kilku minut do godziny. Sprawdzisz ją poleceniem `dig +short book.homeduty.pl`.

## 2. Baza danych

BookList ma własny kontener `booklist-db` (PostgreSQL 17) z danymi w wolumenie `booklist_booklist-db-data`. Baza powstaje sama przy pierwszym starcie z `DB_USERNAME` i `DB_PASSWORD` z `.env`, a tabele tworzy aplikacja. Kontener bazy jest tylko w prywatnej sieci BookList, więc inne aplikacje go nie widzą.

Jeśli BookList używał wcześniej bazy `booklist` we wspólnym kontenerze `postgres`, skrypt `setup-server.sh` przy pierwszym uruchomieniu nowej wersji sam przenosi książki do `booklist-db`. Zostawia przy tym kopię w `~/booklist/booklist-przed-przeniesieniem-*.sql`, a starej bazy nie rusza. Usuwasz ją ręcznie, gdy sprawdzisz, że wszystko jest na miejscu (komendę podaje skrypt).

## 3. Sieć Dockera

Caddy jest w sieci `home-app_default`. BookList dołącza do niej (to domyślna wartość `DOCKER_NETWORK` w `.env.example`), żeby Caddy widział go jako `booklist:8080`. Baza zostaje w prywatnej sieci `booklist_default`.

## 4. Uruchomienie aplikacji

Na serwerze:

```bash
mkdir -p ~/booklist && cd ~/booklist
curl -fsSLO https://raw.githubusercontent.com/popala1986/BookLIst/master/docker-compose.yml
curl -fsSL  https://raw.githubusercontent.com/popala1986/BookLIst/master/.env.example -o .env
nano .env        # uzupełnij hasła (DB_PASSWORD: openssl rand -hex 24) i DOCKER_NETWORK
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

Caddy z home-app wczytuje dodatkowe bloki z folderu `~/home-app/caddy-sites/` (`import /etc/caddy/sites/*.caddy`). Konfiguracja BookList jest w tym repo, w `deploy/booklist.caddy`. Skrypt `setup-server.sh` kopiuje ją tam i przeładowuje Caddy, więc repo home-app nic nie wie o BookList.

Ręcznie:

```bash
mkdir -p ~/home-app/caddy-sites
curl -fsSL https://raw.githubusercontent.com/popala1986/BookLIst/master/deploy/booklist.caddy -o ~/home-app/caddy-sites/booklist.caddy
docker exec caddy caddy reload --config /etc/caddy/Caddyfile
```

Caddy sam pobierze certyfikat Let's Encrypt, gdy rekord DNS już działa. Wejdź na `https://book.homeduty.pl` i zaloguj się danymi z `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD`.

## Aktualizacja

### Automatycznie (po włączeniu)

Po każdym merge do `master` GitHub Actions buduje obraz, a potem łączy się z serwerem przez SSH i uruchamia `~/booklist/update.sh` (pobiera nową wersję i restartuje aplikację). Włączasz to raz:

1. Na serwerze:
   ```bash
   bash <(curl -fsSL https://raw.githubusercontent.com/popala1986/BookLIst/master/deploy/enable-auto-deploy.sh)
   ```
   Skrypt zapisuje `~/booklist/update.sh`, tworzy osobny klucz SSH `~/.ssh/booklist_deploy` i dodaje go do `~/.ssh/authorized_keys` z ograniczeniem `restrict,command=...`, więc ten klucz może uruchomić tylko `update.sh` (bez konsoli i przekierowań).
2. Skrypt wypisuje trzy wartości. Wklej je w https://github.com/popala1986/BookLIst/settings/secrets/actions jako sekrety `DEPLOY_TARGET`, `DEPLOY_KNOWN_HOSTS` i `DEPLOY_SSH_KEY`.

Bez tych sekretów krok „deploy” w Actions jest pomijany. Wyłączenie: usuń sekrety albo linijkę z `booklist-deploy` w `~/.ssh/authorized_keys`.

### Ręcznie

```bash
~/booklist/update.sh
```

albo `cd ~/booklist && docker compose pull && docker compose up -d`.

## Konta użytkowników

- **Administrator** jest jeden. Aplikacja zakłada go przy pierwszym starcie z `APP_ADMIN_USERNAME` i `APP_ADMIN_PASSWORD`. Hasło z `.env` jest użyte tylko wtedy, gdy konta jeszcze nie ma, więc po pierwszym starcie możesz je z `.env` usunąć. Administrator widzi link „Panel admina” z listą kont i liczbą książek. Jeśli zmienisz `APP_ADMIN_USERNAME` na nazwę innego istniejącego konta, to ono stanie się administratorem, a poprzednie straci uprawnienia.
- **Pozostali** zakładają konta sami na stronie „Załóż konto”. Każdy widzi tylko swoje książki.
- Rejestrację wyłączysz, ustawiając `APP_REGISTRATION_ENABLED=false` i restartując kontener (`docker compose up -d`).
- `APP_REMEMBER_ME_KEY` (wygeneruj `openssl rand -hex 32`) sprawia, że opcja „Nie wylogowuj mnie” działa także po restarcie aplikacji.

## Klucz Google Books (okładki)

Klucz jest opcjonalny. Bez niego Google zwykle odrzuca zapytania, a wtedy aplikacja szuka okładki w Open Library, które klucza nie wymaga. Jeśli i tam jej nie ma, książka dostaje okładkę domyślną i przycisk „Szukaj okładki”, żeby spróbować później. Z kluczem Google znajduje więcej okładek, w tym polskich wydań. Klucz jest darmowy, ale Google Cloud wymaga włączonej weryfikacji dwuetapowej na koncie Google:

1. Wejdź na https://console.cloud.google.com/ i utwórz projekt.
2. „APIs & Services” → „Library” → włącz **Books API**.
3. „Credentials” → „Create credentials” → „API key”. Najlepiej ogranicz klucz do Books API.
4. Wpisz go w `.env` jako `GOOGLE_BOOKS_API_KEY` i zrób `docker compose up -d`.

## Kopia zapasowa

```bash
docker exec booklist-db pg_dump -U booklist booklist > booklist-$(date +%F).sql
```

## Zasoby

Aplikacja ma limit 512 MB RAM, a Java używa do 75% tego limitu. Baza ma limit 256 MB i ustawienia na małe zużycie (`shared_buffers=32MB`), w praktyce zajmuje ok. 40–60 MB. Limity są w `docker-compose.yml`.

## Lokalnie (bez Dockera)

Skopiuj `src/main/resources/application-example.properties` do `application.properties`, uzupełnij dane MySQL i konto administratora, a potem `./mvnw spring-boot:run`. Książki dodane przed wprowadzeniem kont zostaną przypisane administratorowi przy pierwszym starcie.
