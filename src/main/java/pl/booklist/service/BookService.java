package pl.booklist.service;

import org.springframework.stereotype.Service;
import pl.booklist.dto.BookDTO;
import pl.booklist.mapper.BookMapper;
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

    private final BookRepository bookRepository;
    private final BookMapper bookMapper; //

    public BookService(BookRepository bookRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    public List<BookDTO> findAllBooks() {
        List<Book> books = bookRepository.findAll();
        return books.stream()
                .map(bookMapper::toDto)
                .toList();
    }

    public List<BookDTO> findOwnedBooks() {
        List<Book> books = bookRepository.findByOwnedTrue();
        return books.stream()
                .map(bookMapper::toDto)
                .toList();
    }

    public List<BookDTO> findUnownedBooks() {
        List<Book> books = bookRepository.findByOwnedFalse();
        return books.stream()
                .map(bookMapper::toDto)
                .toList();
    }
}
