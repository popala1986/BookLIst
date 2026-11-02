package pl.booklist.exception;

/**
 * Thrown when a book with the given id does not exist.
 */
public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(Long id) {
        super("Nie znaleziono książki o ID: " + id);
    }
}
