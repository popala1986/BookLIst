package pl.booklist.service;

import org.springframework.stereotype.Service;
import pl.booklist.model.Book;
import pl.booklist.repository.BookRepository;

import java.util.List;

/**
 * Service layer for managing {@link Book} entities.
 * Provides business logic and access to book-related operations.
 * Delegates persistence task to {@link BookRepository}.
 *
 * @author Paweł Opala
 */
@Service
public class BookService {


    private  final BookRepository bookRepository;

    /**
     * Constructs a new BookService with the given repository.
     * @param bookRepository the repository used for book persistence.
     */
    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Retrieves all books from the database.
     *
     * @return a list of all {@link Book} entities
     */
    public List<Book> findAllBooks() {
        return bookRepository.findAll();
    }
}
