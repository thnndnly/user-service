// TwoFactorSecretRepository.java
package com.elysion.user.repository;

import com.elysion.user.entity.TwoFactorSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TwoFactorSecretRepository extends JpaRepository<TwoFactorSecret, UUID> {
    Optional<TwoFactorSecret> findByUserId(UUID userId);
}