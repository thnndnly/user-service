package com.elysion.user.integration.repository;

import com.elysion.user.entity.MerchantProfile;
import com.elysion.user.entity.User;
import com.elysion.user.repository.MerchantProfileRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")  // lädt application-test.properties
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // keine Embedded-DB
class MerchantProfileRepositoryIntegrationTest {

    @Autowired
    private MerchantProfileRepository profileRepository;

    @Autowired
    private EntityManager em;

    private User createAndPersistUser(String email) {
        User u = User.builder()
                .email(email)
                .passwordHash("pw")
                .name("MerchantUser")
                .isActive(true)
                .isBanned(false)
                .build();
        em.persist(u);
        em.flush();
        return u;
    }

    @Test
    @DisplayName("Ein MerchantProfile anlegen und per findById wiederfinden")
    void testCreateAndFindById() {
        User user = createAndPersistUser("merchant-" + UUID.randomUUID() + "@example.com");

        MerchantProfile profile = MerchantProfile.builder()
                .user(user)
                .companyName("ACME GmbH")
                .taxId("DE123456789")
                .address("Musterstraße 1, 12345 Berlin")
                .website("https://www.acme.de")
                .verified(false)
                .build();

        // Speichern
        profileRepository.save(profile);
        profileRepository.flush();

        // ID wird vom User übernommen
        UUID id = user.getId();
        assertThat(profileRepository.findById(id))
                .isPresent()
                .get()
                .satisfies(p -> {
                    assertThat(p.getCompanyName()).isEqualTo("ACME GmbH");
                    assertThat(p.getTaxId()).isEqualTo("DE123456789");
                    assertThat(p.isVerified()).isFalse();
                    assertThat(p.getUser().getId()).isEqualTo(id);
                });
    }

    @Test
    @DisplayName("findByUserId liefert korrektes Profil")
    void testFindByUserId() {
        User user = createAndPersistUser("merchant2-" + UUID.randomUUID() + "@example.com");

        MerchantProfile profile = MerchantProfile.builder()
                .user(user)
                .companyName("Beispiel AG")
                .taxId("DE987654321")
                .address("Beispielweg 2, 54321 München")
                .website("https://www.beispiel.de")
                .verified(true)
                .build();

        em.persist(profile);
        em.flush();

        assertThat(profileRepository.findByUserId(user.getId()))
                .isPresent()
                .get()
                .extracting(MerchantProfile::getWebsite, MerchantProfile::isVerified)
                .containsExactly("https://www.beispiel.de", true);
    }

    @Test
    @DisplayName("Profil updaten und Änderungen speichern")
    void testUpdateProfile() {
        User user = createAndPersistUser("merchant3-" + UUID.randomUUID() + "@example.com");

        MerchantProfile profile = MerchantProfile.builder()
                .user(user)
                .companyName("Old Co")
                .taxId("DE000000000")
                .address("Old Address")
                .website("http://old.example.com")
                .verified(false)
                .build();

        em.persist(profile);
        em.flush();

        // Status ändern
        profile.setVerified(true);
        profile.setCompanyName("New Co");
        profileRepository.save(profile);
        profileRepository.flush();

        MerchantProfile updated = profileRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getCompanyName()).isEqualTo("New Co");
        assertThat(updated.isVerified()).isTrue();
    }
}