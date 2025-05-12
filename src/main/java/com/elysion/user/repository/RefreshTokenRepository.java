// RefreshTokenRepository.java
package com.elysion.user.repository;

import com.elysion.user.entity.RefreshToken;
import com.elysion.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findByUser(User user);
    void deleteByExpiresAtBefore(Instant now);
}