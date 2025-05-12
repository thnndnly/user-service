package com.elysion.user.integration.repository;

import com.elysion.user.entity.EmailVerificationToken;
import com.elysion.user.entity.User;
import com.elysion.user.repository.EmailVerificationTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")  // lädt application-test.properties
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // keine Embedded-DB
class EmailVerificationTokenRepositoryIntegrationTest {

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private EntityManager em;

    private User createAndPersistUser() {
        User u = User.builder()
                .email("foo@example.com")
                .passwordHash("secret")
                .name("Foo")
                .isActive(true)
                .isBanned(false)
                .build();
        em.persist(u);
        // flush, damit u.id gesetzt wird
        em.flush();
        return u;
    }

    @Test
    @DisplayName("Speichern und Finden nach VerificationToken")
    void testSaveAndFindByVerificationToken() {
        User user = createAndPersistUser();

        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .verificationToken("my-token-123")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        token = tokenRepository.saveAndFlush(token);

        Optional<EmailVerificationToken> found = tokenRepository.findByVerificationToken("my-token-123");
        assertThat(found)
                .isPresent()
                .get()
                .extracting(EmailVerificationToken::getId, EmailVerificationToken::getUser, EmailVerificationToken::getVerificationToken)
                .containsExactly(token.getId(), user, "my-token-123");
    }

    @Test
    @DisplayName("Löschen von abgelaufenen Tokens")
    void testDeleteByExpiresAtBefore() {
        User user = createAndPersistUser();

        // ein abgelaufener Token
        EmailVerificationToken expired = EmailVerificationToken.builder()
                .user(user)
                .verificationToken("old-token")
                .expiresAt(Instant.now().minusSeconds(60))
                .build();

        // ein gültiger Token
        EmailVerificationToken valid = EmailVerificationToken.builder()
                .user(user)
                .verificationToken("new-token")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        em.persist(expired);
        em.persist(valid);
        em.flush();

        // löschen aller, die vor jetzt ablaufen
        tokenRepository.deleteByExpiresAtBefore(Instant.now());
        em.flush();

        // nur der gültige sollte noch da sein
        assertThat(tokenRepository.findByVerificationToken("old-token")).isEmpty();
        assertThat(tokenRepository.findByVerificationToken("new-token")).isPresent();
    }
}
