package _2.LTW.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import _2.LTW.entity.RefreshTokens;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokens, Long> {
    Optional<RefreshTokens> findByJti(String jti);
    void deleteByUser_Id(Long userId);
}
