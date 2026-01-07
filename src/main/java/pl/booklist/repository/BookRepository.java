package pl.booklist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.booklist.model.Book;
import java.util.List;


/**
 * Repository interface for accessing and managing {@link Book} entities.
 * Extends {@link JpaRepository} to provide standard CRUD operations.
 * Used by persistence layer to interreact with the database.
 *
 * @author Paweł Opala
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Retrieves all books that are marked as owned by the user.
     *
     * @return list of books with owned = true.
     */
    List<Book> findByOwnedTrue();

    /**
     * Retrieves all books that are not marked as owned by the user.
     *
     * @return list of books with owned = false.
     */
    List<Book> findByOwnedFalse();
}
