// src/main/java/com/elysion/user/service/TokenCleanupService.java
package com.elysion.user.service;

import com.elysion.user.repository.EmailVerificationTokenRepository;
import com.elysion.user.repository.PasswordResetTokenRepository;
import com.elysion.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    /** Läuft täglich um 1 Uhr und entfernt abgelaufene Tokens */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        refreshTokenRepository.deleteByExpiresAtBefore(now);
        emailTokenRepository.deleteByExpiresAtBefore(now);
        passwordResetTokenRepository.deleteByExpiresAtBefore(now);
    }
}