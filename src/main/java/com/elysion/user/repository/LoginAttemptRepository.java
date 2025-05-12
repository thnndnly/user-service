// LoginAttemptRepository.java
package com.elysion.user.repository;

import com.elysion.user.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {
    List<LoginAttempt> findByIpAddressAndAttemptTimeAfter(String ipAddress, Instant after);
    List<LoginAttempt> findByUserIdAndAttemptTimeAfter(UUID userId, Instant after);
}