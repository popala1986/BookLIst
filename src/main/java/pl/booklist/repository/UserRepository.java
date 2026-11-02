package pl.booklist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.booklist.model.AppUser;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<AppUser> findAllByOrderByCreatedAtAsc();
}
