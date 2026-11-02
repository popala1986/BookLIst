package pl.booklist.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.booklist.dto.BookDTO;
import pl.booklist.exception.BookNotFoundException;
import pl.booklist.mapper.BookMapper;
import pl.booklist.model.AppUser;
import pl.booklist.model.Book;
import pl.booklist.repository.BookRepository;
import pl.booklist.repository.UserRepository;

import java.util.List;

/**
 * Service layer for managing {@link Book} entities.
 * Every operation works on the books of one user, identified by username.
 *
 * @author Paweł Opala
 */
@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookMapper bookMapper;
    private final CoverService coverService;

    public BookService(BookRepository bookRepository, UserRepository userRepository,
                       BookMapper bookMapper, CoverService coverService) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.bookMapper = bookMapper;
        this.coverService = coverService;
    }

    public List<BookDTO> findAllBooks(String username) {
        return bookMapper.toDtoList(bookRepository.findByOwnerUsernameOrderByTitleAsc(username));
    }

    public List<BookDTO> findOwnedBooks(String username) {
        return bookMapper.toDtoList(bookRepository.findByOwnerUsernameAndOwnedOrderByTitleAsc(username, true));
    }

    public List<BookDTO> findUnownedBooks(String username) {
        return bookMapper.toDtoList(bookRepository.findByOwnerUsernameAndOwnedOrderByTitleAsc(username, false));
    }

    /**
     * Saves a new book for the user. When no cover URL is given, the cover is looked up
     * in Google Books and Open Library, falling back to the default cover.
     */
    @Transactional
    public BookDTO addBook(BookDTO bookDTO, String username) {
        AppUser owner = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalStateException("Unknown user: " + username));

        String coverUrl = bookDTO.getCoverUrl();
        if (coverUrl == null || coverUrl.isBlank()) {
            coverUrl = coverService.fetchCoverUrl(bookDTO.getTitle(), bookDTO.getAuthor());
        }
        bookDTO.setCoverUrl(coverUrl.trim());

        Book book = bookMapper.toEntity(bookDTO);
        book.setId(null);
        book.setOwner(owner);
        return bookMapper.toDto(bookRepository.save(book));
    }

    @Transactional
    public void deleteById(Long id, String username) {
        bookRepository.delete(findOwnBook(id, username));
    }

    @Transactional
    public void markAsOwned(Long id, String username) {
        findOwnBook(id, username).setOwned(true);
    }

    /**
     * Looks up the cover again, e.g. for a book that got the default cover when it was added.
     *
     * @return true when a real cover was found
     */
    @Transactional
    public boolean refreshCover(Long id, String username) {
        Book book = findOwnBook(id, username);
        String coverUrl = coverService.fetchCoverUrl(book.getTitle(), book.getAuthor());
        if (CoverService.DEFAULT_COVER_URL.equals(coverUrl)) {
            return false;
        }
        book.setCoverUrl(coverUrl);
        return true;
    }

    private Book findOwnBook(Long id, String username) {
        return bookRepository.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new BookNotFoundException(id));
    }
}
