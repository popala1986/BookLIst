package pl.booklist.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.booklist.model.Book;
import pl.booklist.service.BookService;

import java.util.List;

/**
 * REST controller for handling book-related HTTP requests.
 * Provides endpoints for retrieving book data.
 * </p>
 * This controller is validated and uses {@link BookService} to access book entities.
 * </p>
 *
 * @author Paweł
 */
@RestController
@Validated
public class BookController {

    private final BookService bookService;


    /**
     * Constructs a new {@code BookController} with the given {@link BookService}.
     *
     * @param bookService the service used to manage book entities.
     */
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * Handles GET request to retrieve all books.
     * @return a list of all {@link Book} entities from the database.
     */
    @GetMapping("/showAllBooks")
    public List<Book> getAllBooks() {
        return bookService.findAllBooks();
    }

    /**
     * Handles GET request to retrieve all books marked as owned by the user.
     *
     * @return a list of owned {@link Book} entities.
     */
    @GetMapping("/my-books")
    public List<Book> getOwnedBooks() {
        return bookService.findOwnedBooks();
    }
}
