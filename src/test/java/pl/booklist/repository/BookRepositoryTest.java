package pl.booklist.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pl.booklist.model.AppUser;
import pl.booklist.model.Book;
import pl.booklist.model.Role;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    private AppUser anna;
    private AppUser jan;

    @BeforeEach
    void setUp() {
        anna = userRepository.save(new AppUser("anna", "{noop}x", Role.USER));
        jan = userRepository.save(new AppUser("jan", "{noop}x", Role.USER));
    }

    private Book book(String title, boolean owned, AppUser owner) {
        Book book = new Book(null, title, "Autor", owned, "http://cover.url");
        book.setOwner(owner);
        return bookRepository.save(book);
    }

    @Test
    @DisplayName("GIVEN books of two users, WHEN listing owned books, THEN only the user's owned books are returned, sorted by title")
    void shouldFindOwnedBooksOfUser() {
        book("Effective Java", true, anna);
        book("Clean Code", true, anna);
        book("Lód", false, anna);
        book("Wiedźmin", true, jan);

        List<Book> owned = bookRepository.findByOwnerUsernameAndOwnedOrderByTitleAsc("anna", true);

        assertThat(owned).extracting(Book::getTitle).containsExactly("Clean Code", "Effective Java");
    }

    @Test
    @DisplayName("GIVEN books of two users, WHEN listing the wishlist, THEN only the user's unowned books are returned")
    void shouldFindWishlistOfUser() {
        book("Clean Code", true, anna);
        book("Lód", false, anna);
        book("Solaris", false, jan);

        List<Book> wishlist = bookRepository.findByOwnerUsernameAndOwnedOrderByTitleAsc("anna", false);

        assertThat(wishlist).extracting(Book::getTitle).containsExactly("Lód");
    }

    @Test
    @DisplayName("A user cannot load another user's book by id")
    void shouldNotFindOtherUsersBook() {
        Book jansBook = book("Solaris", false, jan);

        assertThat(bookRepository.findByIdAndOwnerUsername(jansBook.getId(), "anna")).isEmpty();
        assertThat(bookRepository.findByIdAndOwnerUsername(jansBook.getId(), "jan")).isPresent();
    }

    @Test
    @DisplayName("Books without an owner are assigned to the given user")
    void shouldAssignOwnerlessBooks() {
        book("Stara książka", true, null);
        book("Clean Code", true, jan);

        int assigned = bookRepository.assignOwnerlessBooksTo(anna);

        assertThat(assigned).isEqualTo(1);
        assertThat(bookRepository.findByOwnerUsernameOrderByTitleAsc("anna")).extracting(Book::getTitle)
                .containsExactly("Stara książka");
    }
}
