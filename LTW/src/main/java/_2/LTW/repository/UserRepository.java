package _2.LTW.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import _2.LTW.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}
