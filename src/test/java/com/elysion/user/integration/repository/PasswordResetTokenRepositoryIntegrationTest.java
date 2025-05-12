package com.elysion.user.integration.repository;

import com.elysion.user.entity.PasswordResetToken;
import com.elysion.user.entity.User;
import com.elysion.user.repository.PasswordResetTokenRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")  // lädt application-test.properties
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // keine Embedded-DB
class PasswordResetTokenRepositoryIntegrationTest {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private EntityManager em;

    private User createAndPersistUser(String email) {
        User u = User.builder()
                .email(email)
                .passwordHash("pw")
                .name("ResetUser")
                .isActive(true)
                .isBanned(false)
                .build();
        em.persist(u);
        em.flush();
        return u;
    }

    @Test
    @DisplayName("Token speichern und per findByResetToken wiederfinden")
    void testSaveAndFindByResetToken() {
        User user = createAndPersistUser("reset-" + UUID.randomUUID() + "@example.com");
        String tokenValue = "tok-" + UUID.randomUUID();

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .resetToken(tokenValue)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        tokenRepository.save(token);
        tokenRepository.flush();

        Optional<PasswordResetToken> found = tokenRepository.findByResetToken(tokenValue);
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(found.get().getResetToken()).isEqualTo(tokenValue);
    }

    @Test
    @DisplayName("deleteByExpiresAtBefore entfernt nur abgelaufene Tokens")
    void testDeleteByExpiresAtBefore() {
        User user = createAndPersistUser("reset2-" + UUID.randomUUID() + "@example.com");

        // abgelaufener Token
        PasswordResetToken expired = PasswordResetToken.builder()
                .user(user)
                .resetToken("expired-" + UUID.randomUUID())
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();

        // noch gültiger Token
        PasswordResetToken valid = PasswordResetToken.builder()
                .user(user)
                .resetToken("valid-" + UUID.randomUUID())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        em.persist(expired);
        em.persist(valid);
        em.flush();

        // lösche alle, die vor jetzt abgelaufen sind
        tokenRepository.deleteByExpiresAtBefore(Instant.now());
        tokenRepository.flush();

        // expired muss weg sein…
        assertThat(tokenRepository.findByResetToken(expired.getResetToken()))
                .isNotPresent();

        // …valid muss noch da sein
        assertThat(tokenRepository.findByResetToken(valid.getResetToken()))
                .isPresent();
    }
}