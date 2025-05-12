// EmailVerificationTokenRepository.java
package com.elysion.user.repository;

import com.elysion.user.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByVerificationToken(String token);
    void deleteByExpiresAtBefore(Instant now);
}