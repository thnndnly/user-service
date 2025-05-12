package com.elysion.user.integration.repository;

import com.elysion.user.entity.RefreshToken;
import com.elysion.user.entity.User;
import com.elysion.user.repository.RefreshTokenRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")  // lädt application-test.properties
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // keine Embedded-DB
class RefreshTokenRepositoryIntegrationTest {

    @Autowired
    private RefreshTokenRepository tokenRepository;

    @Autowired
    private EntityManager em;

    private User createAndPersistUser(String email) {
        User u = User.builder()
                .email(email)
                .passwordHash("pw")
                .name("RTUser")
                .isActive(true)
                .isBanned(false)
                .build();
        em.persist(u);
        em.flush();
        return u;
    }

    @Test
    @DisplayName("Token speichern und per findByTokenHash wiederfinden")
    void testSaveAndFindByTokenHash() {
        User user = createAndPersistUser("rt-" + UUID.randomUUID() + "@example.com");
        String hash = "hash-" + UUID.randomUUID();

        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        tokenRepository.save(rt);
        tokenRepository.flush();

        Optional<RefreshToken> found = tokenRepository.findByTokenHash(hash);
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(found.get().getTokenHash()).isEqualTo(hash);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByUser liefert alle Tokens eines Nutzers")
    void testFindByUser() {
        User user1 = createAndPersistUser("u1-" + UUID.randomUUID() + "@example.com");
        User user2 = createAndPersistUser("u2-" + UUID.randomUUID() + "@example.com");

        RefreshToken rt1 = RefreshToken.builder()
                .user(user1)
                .tokenHash("h1-" + UUID.randomUUID())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        RefreshToken rt2 = RefreshToken.builder()
                .user(user1)
                .tokenHash("h2-" + UUID.randomUUID())
                .expiresAt(Instant.now().plusSeconds(7200))
                .build();

        RefreshToken rt3 = RefreshToken.builder()
                .user(user2)
                .tokenHash("h3-" + UUID.randomUUID())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        em.persist(rt1);
        em.persist(rt2);
        em.persist(rt3);
        em.flush();

        List<RefreshToken> list1 = tokenRepository.findByUser(user1);
        assertThat(list1).hasSize(2)
                .extracting(RefreshToken::getTokenHash)
                .containsExactlyInAnyOrder(rt1.getTokenHash(), rt2.getTokenHash());

        List<RefreshToken> list2 = tokenRepository.findByUser(user2);
        assertThat(list2).hasSize(1)
                .extracting(RefreshToken::getTokenHash)
                .containsExactly(rt3.getTokenHash());
    }

    @Test
    @DisplayName("deleteByExpiresAtBefore entfernt nur abgelaufene Tokens")
    void testDeleteByExpiresAtBefore() {
        User user = createAndPersistUser("del-" + UUID.randomUUID() + "@example.com");

        RefreshToken expired = RefreshToken.builder()
                .user(user)
                .tokenHash("expired-" + UUID.randomUUID())
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();

        RefreshToken valid = RefreshToken.builder()
                .user(user)
                .tokenHash("valid-" + UUID.randomUUID())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        em.persist(expired);
        em.persist(valid);
        em.flush();

        tokenRepository.deleteByExpiresAtBefore(Instant.now());
        tokenRepository.flush();

        assertThat(tokenRepository.findByTokenHash(expired.getTokenHash())).isNotPresent();
        assertThat(tokenRepository.findByTokenHash(valid.getTokenHash())).isPresent();
    }
}
