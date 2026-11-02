# BookLIst

Aplikacja do zarządzania własną kolekcją książek i listą życzeń. Okładki pobiera automatycznie z Google Books, a gdy tam ich nie ma, z Open Library.

**Funkcje:** konta użytkowników (każdy widzi tylko swoje książki), jedno konto administratora z panelem, lista życzeń i kolekcja w zakładkach, wyszukiwarka, podgląd okładki przy dodawaniu, tryb jasny i ciemny, wersja na telefon.

**Stack:** Java 17, Spring Boot 3.5 (Web, Data JPA, Security, Validation, Thymeleaf), MapStruct, PostgreSQL (produkcja) / MySQL (lokalnie), H2 w testach, Docker.

## Uruchomienie lokalne

1. Skopiuj `src/main/resources/application-example.properties` do `application.properties` i uzupełnij dane bazy oraz konto administratora (`app.admin.username`, `app.admin.password`).
2. `./mvnw spring-boot:run`
3. Otwórz http://localhost:8080 i zaloguj się jako administrator albo załóż nowe konto.

## Testy

```bash
./mvnw test
```

Testy używają bazy H2 w pamięci, więc nie potrzebują MySQL.

## REST API

Wymaga HTTP Basic (te same dane co do logowania). Każdy użytkownik widzi i zmienia tylko swoje książki.

| Metoda | Ścieżka | Opis |
|--------|---------|------|
| GET | `/api/books/showAllBooks` | wszystkie książki |
| GET | `/api/books/my-books` | kolekcja |
| GET | `/api/books/wishlist` | lista życzeń |
| POST | `/api/books` | dodanie książki |
| DELETE | `/api/books/{id}` | usunięcie książki |

## Wdrożenie

Zobacz [DEPLOY.md](DEPLOY.md).
