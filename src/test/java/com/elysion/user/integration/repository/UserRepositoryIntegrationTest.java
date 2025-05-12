package com.elysion.user.integration.repository;

import com.elysion.user.entity.User;
import com.elysion.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import java.time.temporal.ChronoUnit;

@DataJpaTest
@ActiveProfiles("test")  // lädt application-test.properties
@AutoConfigureTestDatabase(replace = Replace.NONE) // keine Embedded-DB
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("save & findByEmail liefert vorhandenen User")
    void testSaveAndFindByEmail() {
        User u = User.builder()
                .email("alice@example.com")
                .passwordHash("pw")
                .name("Alice")
                .roles(Set.of())
                .build();

        // speichern
        User saved = userRepository.save(u);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isCloseTo(Instant.now(), within(3, ChronoUnit.SECONDS));

        // findByEmail
        Optional<User> opt = userRepository.findByEmail("alice@example.com");
        assertThat(opt).isPresent();
        User found = opt.get();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("findByEmail gibt leer zurück, wenn nicht existiert")
    void testFindByEmailNotFound() {
        Optional<User> opt = userRepository.findByEmail("noone@nowhere.com");
        assertThat(opt).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail erkennt vorhandene E-Mail korrekt")
    void testExistsByEmail() {
        User u = User.builder()
                .email("bob@example.com")
                .passwordHash("pw2")
                .roles(Set.of())
                .build();
        userRepository.save(u);

        assertThat(userRepository.existsByEmail("bob@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("other@example.com")).isFalse();
    }

    @Test
    @DisplayName("unique constraint auf email schlägt bei Doppel an")
    void testUniqueEmailConstraint() {
        User u1 = User.builder()
                .email("dup@example.com")
                .passwordHash("h1")
                .roles(Set.of())
                .build();
        userRepository.saveAndFlush(u1);

        User u2 = User.builder()
                .email("dup@example.com")
                .passwordHash("h2")
                .roles(Set.of())
                .build();
        assertThatThrownBy(() -> userRepository.saveAndFlush(u2))
                .isInstanceOfAny(
                        org.springframework.dao.DataIntegrityViolationException.class,
                        jakarta.persistence.PersistenceException.class
                );
    }
}
