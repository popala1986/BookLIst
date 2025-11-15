package pl.booklist.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pl.booklist.model.Book;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    @DisplayName("GIVEN two books, WHEN findByOwnedTrue is called, THEN only one owned book is returned")
    void shouldFindOwnedBooks() {
        // GIVEN
        Book ownedBook = new Book(null, "Clean Code", "Robert C. Martin", true, "http://cover.url");
        Book notOwnedBook = new Book(null, "Effective Java", "Joshua Bloch", false, "http://cover.url");

        bookRepository.save(ownedBook);
        bookRepository.save(notOwnedBook);

        // WHEN
        List<Book> ownedBooks = bookRepository.findByOwnedTrue();

        // THEN
        assertThat(ownedBooks).as("It should find exactly one owned book.").hasSize(1);
        assertThat(ownedBooks.get(0).getTitle()).isEqualTo("Clean Code");
        assertThat(ownedBooks.get(0).getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(ownedBooks.get(0).getCoverUrl()).isEqualTo("http://cover.url");
    }

    @Test
    @DisplayName("GIVEN two books, WHEN findByOwnedFalse is called, THEN only one unowned book is returned")
    void shouldFindNotOwnedBooks() {
        // GIVEN
        Book ownedBook = new Book(null, "Clean Code", "Robert C. Martin", true, "http://cover.url");
        Book notOwnedBook = new Book(null, "Effective Java", "Joshua Bloch", false, "http://cover.url");

        bookRepository.save(ownedBook);
        bookRepository.save(notOwnedBook);

        // WHEN
        List<Book > notOwnedBooks = bookRepository.findByOwnedFalse();

        // THEN
        assertThat(notOwnedBooks).as("It should find exactly one unowned book.").hasSize(1);
        assertThat(notOwnedBooks.get(0).getTitle()).isEqualTo("Effective Java");
        assertThat(notOwnedBooks.get(0).isOwned()).isFalse();
    }
}