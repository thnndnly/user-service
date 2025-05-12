// PasswordResetTokenRepository.java
package com.elysion.user.repository;

import com.elysion.user.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByResetToken(String token);
    void deleteByExpiresAtBefore(Instant now);
}