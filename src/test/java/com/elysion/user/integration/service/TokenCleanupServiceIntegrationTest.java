package com.elysion.user.integration.service;

import com.elysion.user.entity.*;
import com.elysion.user.repository.*;
import com.elysion.user.service.TokenCleanupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")  // application-test.properties
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TokenCleanupService.class)
class TokenCleanupServiceIntegrationTest {

    @Autowired
    private TokenCleanupService cleanupService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EmailVerificationTokenRepository emailTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private User makeUser() {
        User u = new User();
        u.setEmail("junit@example.org");
        u.setPasswordHash("irrelevant");
        u.setName("JUnit");
        u.setActive(true);
        u.setBanned(false);
        u.setRoles(new HashSet<>());
        return userRepository.save(u);
    }

    @Test
    @DisplayName("cleanupExpiredTokens deletes only expired tokens")
    void cleanupExpiredTokensRemovesExpiredOnly() {
        User user = makeUser();
        Instant now = Instant.now();

        // 1) lege jeweils einen expired und einen valid Token an

        // --- RefreshToken ---
        RefreshToken expiredRt = new RefreshToken();
        expiredRt.setUser(user);
        expiredRt.setTokenHash("expired-rt");
        expiredRt.setExpiresAt(now.minusSeconds(60));
        refreshTokenRepository.save(expiredRt);

        RefreshToken validRt = new RefreshToken();
        validRt.setUser(user);
        validRt.setTokenHash("valid-rt");
        validRt.setExpiresAt(now.plusSeconds(3600));
        refreshTokenRepository.save(validRt);

        // --- EmailVerificationToken ---
        EmailVerificationToken expiredEv = new EmailVerificationToken();
        expiredEv.setUser(user);
        expiredEv.setVerificationToken("expired-ev");
        expiredEv.setExpiresAt(now.minusSeconds(60));
        emailTokenRepository.save(expiredEv);

        EmailVerificationToken validEv = new EmailVerificationToken();
        validEv.setUser(user);
        validEv.setVerificationToken("valid-ev");
        validEv.setExpiresAt(now.plusSeconds(3600));
        emailTokenRepository.save(validEv);

        // --- PasswordResetToken ---
        PasswordResetToken expiredPr = new PasswordResetToken();
        expiredPr.setUser(user);
        expiredPr.setResetToken("expired-pr");
        expiredPr.setExpiresAt(now.minusSeconds(60));
        passwordResetTokenRepository.save(expiredPr);

        PasswordResetToken validPr = new PasswordResetToken();
        validPr.setUser(user);
        validPr.setResetToken("valid-pr");
        validPr.setExpiresAt(now.plusSeconds(3600));
        passwordResetTokenRepository.save(validPr);

        // 2) Service ausführen
        cleanupService.cleanupExpiredTokens();

        // 3) Assertions: expired weg, gültige da
        assertThat(refreshTokenRepository.findByTokenHash("expired-rt")).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash("valid-rt")).isPresent();

        assertThat(emailTokenRepository.findByVerificationToken("expired-ev")).isEmpty();
        assertThat(emailTokenRepository.findByVerificationToken("valid-ev")).isPresent();

        assertThat(passwordResetTokenRepository.findByResetToken("expired-pr")).isEmpty();
        assertThat(passwordResetTokenRepository.findByResetToken("valid-pr")).isPresent();
    }
}