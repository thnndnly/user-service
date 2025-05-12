package com.elysion.user.integration.repository;

import com.elysion.user.entity.LoginAttempt;
import com.elysion.user.entity.User;
import com.elysion.user.repository.LoginAttemptRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")  // lädt application-test.properties
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // keine Embedded-DB
class LoginAttemptRepositoryIntegrationTest {

    @Autowired
    private LoginAttemptRepository attemptRepository;

    @Autowired
    private EntityManager em;

    private User createAndPersistUser(String email) {
        User u = User.builder()
                .email(email)
                .passwordHash("pw")
                .name("User-" + email)
                .isActive(true)
                .isBanned(false)
                .build();
        em.persist(u);
        em.flush();
        return u;
    }

    @Test
    @DisplayName("findByIpAddressAndAttemptTimeAfter liefert nur nach Zeit gefilterte Einträge")
    void testFindByIpAddressAndAttemptTimeAfter() {
        Instant cutoff = Instant.now().minusSeconds(300);

        // LoginAttempt ohne User, unterschiedliche Times
        LoginAttempt oldSameIp = LoginAttempt.builder()
                .ipAddress("1.2.3.4")
                .wasSuccessful(false)
                .build();
        em.persist(oldSameIp);
        oldSameIp.setAttemptTime(cutoff.minusSeconds(10));
        em.merge(oldSameIp);

        LoginAttempt recentSameIp = LoginAttempt.builder()
                .ipAddress("1.2.3.4")
                .wasSuccessful(true)
                .build();
        em.persist(recentSameIp);
        recentSameIp.setAttemptTime(cutoff.plusSeconds(10));
        em.merge(recentSameIp);

        LoginAttempt recentOtherIp = LoginAttempt.builder()
                .ipAddress("9.9.9.9")
                .wasSuccessful(false)
                .build();
        em.persist(recentOtherIp);
        recentOtherIp.setAttemptTime(cutoff.plusSeconds(20));
        em.merge(recentOtherIp);

        em.flush();

        List<LoginAttempt> result = attemptRepository.findByIpAddressAndAttemptTimeAfter("1.2.3.4", cutoff);
        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(a -> {
                    assertThat(a.getIpAddress()).isEqualTo("1.2.3.4");
                    assertThat(a.getAttemptTime()).isAfter(cutoff);
                });
    }

    @Test
    @DisplayName("findByUserIdAndAttemptTimeAfter liefert nur nach Zeit gefilterte Einträge für einen User")
    void testFindByUserIdAndAttemptTimeAfter() {
        // zwei verschiedene User mit unterschiedlichen Emails
        User userA = createAndPersistUser("a-" + UUID.randomUUID() + "@example.com");
        User userB = createAndPersistUser("b-" + UUID.randomUUID() + "@example.com");
        Instant cutoff = Instant.now().minusSeconds(600);

        // Eintrag VOR cutoff für userA
        LoginAttempt oldForUserA = LoginAttempt.builder()
                .user(userA)
                .ipAddress("5.5.5.5")
                .wasSuccessful(true)
                .build();
        em.persist(oldForUserA);
        oldForUserA.setAttemptTime(cutoff.minusSeconds(30));
        em.merge(oldForUserA);

        // Eintrag NACH cutoff für userA
        LoginAttempt recentForUserA = LoginAttempt.builder()
                .user(userA)
                .ipAddress("5.5.5.5")
                .wasSuccessful(false)
                .build();
        em.persist(recentForUserA);
        recentForUserA.setAttemptTime(cutoff.plusSeconds(30));
        em.merge(recentForUserA);

        // Eintrag NACH cutoff für userB (soll nicht zurückkommen)
        LoginAttempt recentForUserB = LoginAttempt.builder()
                .user(userB)
                .ipAddress("5.5.5.5")
                .wasSuccessful(true)
                .build();
        em.persist(recentForUserB);
        recentForUserB.setAttemptTime(cutoff.plusSeconds(40));
        em.merge(recentForUserB);

        em.flush();

        List<LoginAttempt> result = attemptRepository.findByUserIdAndAttemptTimeAfter(userA.getId(), cutoff);
        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(a -> {
                    assertThat(a.getUser().getId()).isEqualTo(userA.getId());
                    assertThat(a.getAttemptTime()).isAfter(cutoff);
                });
    }
}