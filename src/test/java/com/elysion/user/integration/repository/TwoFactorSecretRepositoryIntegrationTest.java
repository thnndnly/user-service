package com.elysion.user.integration.repository;

import com.elysion.user.entity.TwoFactorSecret;
import com.elysion.user.entity.User;
import com.elysion.user.repository.TwoFactorSecretRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")  // lädt application-test.properties
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // keine Embedded-DB
class TwoFactorSecretRepositoryIntegrationTest {

    @Autowired
    private TwoFactorSecretRepository secretRepository;

    @Autowired
    private EntityManager em;

    private User createAndPersistUser(String email) {
        User u = User.builder()
                .email(email)
                .passwordHash("pw")
                .name("2FUser")
                .isActive(true)
                .isBanned(false)
                .build();
        em.persist(u);
        em.flush();
        return u;
    }

    @Test
    @DisplayName("Zwei-Faktor-Secret speichern und per findByUserId wiederfinden")
    void testSaveAndFindByUserId() {
        User user = createAndPersistUser("2fa-" + UUID.randomUUID() + "@example.com");

        String totp = "BASE32SECRET";
        String codesJson = "[\"code1\",\"code2\",\"code3\"]";

        TwoFactorSecret secret = TwoFactorSecret.builder()
                .user(user)
                .totpSecret(totp)
                .isEnabled(true)
                .recoveryCodes(codesJson)
                .build();

        secretRepository.save(secret);
        secretRepository.flush();

        Optional<TwoFactorSecret> found = secretRepository.findByUserId(user.getId());
        assertThat(found).isPresent();

        TwoFactorSecret s = found.get();
        assertThat(s.getUser().getId()).isEqualTo(user.getId());
        assertThat(s.getTotpSecret()).isEqualTo(totp);
        assertThat(s.isEnabled()).isTrue();
        assertThat(s.getRecoveryCodes()).isEqualTo(codesJson);
    }

    @Test
    @DisplayName("findByUserId liefert empty, wenn kein Secret existiert")
    void testFindByUserIdNotFound() {
        UUID randomId = UUID.randomUUID();
        Optional<TwoFactorSecret> empty = secretRepository.findByUserId(randomId);
        assertThat(empty).isNotPresent();
    }
}