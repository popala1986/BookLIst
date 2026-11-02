package pl.booklist.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.booklist.dto.BookDTO;
import pl.booklist.exception.BookNotFoundException;
import pl.booklist.mapper.BookMapper;
import pl.booklist.model.AppUser;
import pl.booklist.model.Book;
import pl.booklist.model.Role;
import pl.booklist.repository.BookRepository;
import pl.booklist.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    private static final String USER = "anna";

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private CoverService coverService;

    @InjectMocks
    private BookService bookService;

    private final Book bookEntity1 = new Book(1L, "Wiedźmin", "Sapkowski", true, "url1");
    private final Book bookEntity2 = new Book(2L, "Lód", "Dukaj", false, "url2");

    private final BookDTO bookDto1 = new BookDTO(1L, "Wiedźmin", "Sapkowski", true, "url1");
    private final BookDTO bookDto2 = new BookDTO(2L, "Lód", "Dukaj", false, "url2");

    private final AppUser anna = new AppUser(USER, "{noop}x", Role.USER);

    @Test
    @DisplayName("should list all books of the user")
    void findAllBooks_shouldReturnUsersBooks() {
        List<Book> entities = List.of(bookEntity1, bookEntity2);
        when(bookRepository.findByOwnerUsernameOrderByTitleAsc(USER)).thenReturn(entities);
        when(bookMapper.toDtoList(entities)).thenReturn(List.of(bookDto1, bookDto2));

        assertThat(bookService.findAllBooks(USER)).containsExactly(bookDto1, bookDto2);
    }

    @Test
    @DisplayName("should list owned books of the user")
    void findOwnedBooks_shouldQueryOwned() {
        when(bookRepository.findByOwnerUsernameAndOwnedOrderByTitleAsc(USER, true)).thenReturn(List.of(bookEntity1));
        when(bookMapper.toDtoList(List.of(bookEntity1))).thenReturn(List.of(bookDto1));

        assertThat(bookService.findOwnedBooks(USER)).containsExactly(bookDto1);
    }

    @Test
    @DisplayName("should list the wishlist of the user")
    void findUnownedBooks_shouldQueryUnowned() {
        when(bookRepository.findByOwnerUsernameAndOwnedOrderByTitleAsc(USER, false)).thenReturn(List.of(bookEntity2));
        when(bookMapper.toDtoList(List.of(bookEntity2))).thenReturn(List.of(bookDto2));

        assertThat(bookService.findUnownedBooks(USER)).containsExactly(bookDto2);
    }

    @Test
    @DisplayName("should fetch a cover when none is given and assign the book to the user")
    void addBook_shouldFetchCoverWhenMissing() {
        BookDTO input = new BookDTO("Lód", "Dukaj", false, null);
        Book entity = new Book(null, "Lód", "Dukaj", false, null);
        when(userRepository.findByUsernameIgnoreCase(USER)).thenReturn(Optional.of(anna));
        when(coverService.fetchCoverUrl("Lód", "Dukaj")).thenReturn("https://cover");
        when(bookMapper.toEntity(input)).thenReturn(entity);
        when(bookRepository.save(entity)).thenReturn(entity);
        when(bookMapper.toDto(entity)).thenReturn(bookDto2);

        BookDTO result = bookService.addBook(input, USER);

        assertThat(input.getCoverUrl()).isEqualTo("https://cover");
        assertThat(entity.getOwner()).isSameAs(anna);
        assertThat(result).isEqualTo(bookDto2);
    }

    @Test
    @DisplayName("should not call the cover service when a cover is given")
    void addBook_shouldKeepGivenCover() {
        BookDTO input = new BookDTO("Lód", "Dukaj", false, "https://my-cover");
        Book entity = new Book(null, "Lód", "Dukaj", false, "https://my-cover");
        when(userRepository.findByUsernameIgnoreCase(USER)).thenReturn(Optional.of(anna));
        when(bookMapper.toEntity(input)).thenReturn(entity);
        when(bookRepository.save(entity)).thenReturn(entity);

        bookService.addBook(input, USER);

        verifyNoInteractions(coverService);
        assertThat(input.getCoverUrl()).isEqualTo("https://my-cover");
    }

    @Test
    @DisplayName("should mark the user's book as owned")
    void markAsOwned_shouldSetOwnedFlag() {
        Book book = new Book(2L, "Lód", "Dukaj", false, "url2");
        when(bookRepository.findByIdAndOwnerUsername(2L, USER)).thenReturn(Optional.of(book));

        bookService.markAsOwned(2L, USER);

        assertThat(book.isOwned()).isTrue();
    }

    @Test
    @DisplayName("should throw when the book does not exist or belongs to someone else")
    void markAsOwned_shouldThrowWhenNotUsersBook() {
        when(bookRepository.findByIdAndOwnerUsername(99L, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.markAsOwned(99L, USER)).isInstanceOf(BookNotFoundException.class);
    }

    @Test
    @DisplayName("should delete the user's book")
    void deleteById_shouldDeleteUsersBook() {
        when(bookRepository.findByIdAndOwnerUsername(1L, USER)).thenReturn(Optional.of(bookEntity1));

        bookService.deleteById(1L, USER);

        verify(bookRepository).delete(bookEntity1);
    }

    @Test
    @DisplayName("should not delete a book that is not the user's")
    void deleteById_shouldThrowWhenNotUsersBook() {
        when(bookRepository.findByIdAndOwnerUsername(99L, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.deleteById(99L, USER)).isInstanceOf(BookNotFoundException.class);
        verify(bookRepository, never()).delete(any());
    }

    @Test
    @DisplayName("refreshCover saves a newly found cover")
    void refreshCover_savesFoundCover() {
        Book book = new Book(5L, "Robinson Kruzoe", "Defoe", false, CoverService.DEFAULT_COVER_URL);
        when(bookRepository.findByIdAndOwnerUsername(5L, USER)).thenReturn(Optional.of(book));
        when(coverService.fetchCoverUrl("Robinson Kruzoe", "Defoe")).thenReturn("https://covers.openlibrary.org/b/id/1-L.jpg");

        assertThat(bookService.refreshCover(5L, USER)).isTrue();
        assertThat(book.getCoverUrl()).isEqualTo("https://covers.openlibrary.org/b/id/1-L.jpg");
    }

    @Test
    @DisplayName("refreshCover keeps the book unchanged when nothing is found")
    void refreshCover_nothingFound() {
        Book book = new Book(5L, "Nieznana", "Nikt", false, CoverService.DEFAULT_COVER_URL);
        when(bookRepository.findByIdAndOwnerUsername(5L, USER)).thenReturn(Optional.of(book));
        when(coverService.fetchCoverUrl("Nieznana", "Nikt")).thenReturn(CoverService.DEFAULT_COVER_URL);

        assertThat(bookService.refreshCover(5L, USER)).isFalse();
        assertThat(book.getCoverUrl()).isEqualTo(CoverService.DEFAULT_COVER_URL);
    }

    @Test
    @DisplayName("refreshCover rejects another user's book")
    void refreshCover_otherUsersBook() {
        when(bookRepository.findByIdAndOwnerUsername(9L, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.refreshCover(9L, USER)).isInstanceOf(BookNotFoundException.class);
        verifyNoInteractions(coverService);
    }
}
