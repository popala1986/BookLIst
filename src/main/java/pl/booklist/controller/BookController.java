package pl.booklist.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pl.booklist.dto.BookDTO;
import pl.booklist.service.BookService;

import java.util.List;

/**
 * REST controller for handling book-related HTTP requests.
 * Provides endpoints for retrieving book data.
 * Uses {@link BookService} to access book DTOs.
 *
 * @author Paweł
 */
@RestController
@RequestMapping("/api/books")
@Validated
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * Handles GET request to retrieve all books.
     * @return a list of all {@link BookDTO} objects.
     */
    @GetMapping("/showAllBooks")
    public List<BookDTO> getAllBooks() {
        return bookService.findAllBooks();
    }

    /**
     * Handles GET request to retrieve all books marked as owned by the user.
     * @return a list of owned {@link BookDTO} objects.
     */
    @GetMapping("/my-books")
    public List<BookDTO> getOwnedBooks() {
        return bookService.findOwnedBooks();
    }

    /**
     * Handles GET request to retrieve all books that are not marked as owned by the user.
     * @return a list of unowned {@link BookDTO} objects.
     */
    @GetMapping("/wishlist")
    public List<BookDTO> getUnownedBooks() {
        return bookService.findUnownedBooks();
    }



    @PostMapping
    public ResponseEntity<BookDTO> createBook(@RequestBody BookDTO bookDTO) {

        BookDTO createdBook = bookService.addBook(bookDTO);
        return new ResponseEntity<>(createdBook, HttpStatus.CREATED);
    }




}
