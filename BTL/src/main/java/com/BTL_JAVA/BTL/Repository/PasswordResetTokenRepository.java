package com.BTL_JAVA.BTL.Repository;

import com.BTL_JAVA.BTL.Entity.PasswordResetToken;
import com.BTL_JAVA.BTL.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
	Optional<PasswordResetToken> findByTokenHash(String tokenHash);

	void deleteByUser(User user);
}

