package pl.booklist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.booklist.model.Book;


/**
 * Repository interface for accessing and managing {@link Book} entities.
 * Extends {@link JpaRepository} to provide standard CRUD operations.
 * Used by persistence layer to interreact with the database.
 *
 * @author Paweł Opala
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

}
