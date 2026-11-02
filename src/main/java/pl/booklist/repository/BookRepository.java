package pl.booklist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.booklist.model.AppUser;
import pl.booklist.model.Book;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Book} entities. Every query is scoped to the owning user,
 * so one user can never see or change another user's books.
 *
 * @author Paweł Opala
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByOwnerUsernameOrderByTitleAsc(String username);

    /**
     * @param owned true for the collection, false for the wishlist
     */
    List<Book> findByOwnerUsernameAndOwnedOrderByTitleAsc(String username, boolean owned);

    Optional<Book> findByIdAndOwnerUsername(Long id, String username);

    long countByOwnerAndOwned(AppUser owner, boolean owned);

    /**
     * Assigns books created before user accounts existed (no owner) to the given user.
     *
     * @return number of books assigned
     */
    @Modifying
    @Query("update Book b set b.owner = :owner where b.owner is null")
    int assignOwnerlessBooksTo(@Param("owner") AppUser owner);
}
