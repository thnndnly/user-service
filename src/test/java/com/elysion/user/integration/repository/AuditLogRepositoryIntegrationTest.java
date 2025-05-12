package com.elysion.user.integration.repository;

import com.elysion.user.entity.AuditLog;
import com.elysion.user.entity.User;
import com.elysion.user.repository.AuditLogRepository;
import com.elysion.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import java.time.temporal.ChronoUnit;

@DataJpaTest
@ActiveProfiles("test")  // lädt application-test.properties
@AutoConfigureTestDatabase(replace = Replace.NONE) // keine Embedded-DB
class AuditLogRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private User createUser() {
        User u = User.builder()
                .email("eve@example.com")
                .passwordHash("secret")
                .name("Eve")
                .roles(Set.of())
                .build();
        return userRepository.saveAndFlush(u);
    }

    @Test
    @DisplayName("AuditLog speichern und Metadata korrekt abrufen")
    void testSaveAndRetrieveMetadata() {
        User u = createUser();

        Map<String, Object> data = Map.of("ip", "127.0.0.1", "success", true);
        AuditLog log = AuditLog.builder()
                .user(u)
                .action("LOGIN")
                .metadata(data)
                .build();
        AuditLog saved = auditLogRepository.saveAndFlush(log);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt())
                .isCloseTo(Instant.now(), within(3, ChronoUnit.SECONDS));

        AuditLog found = auditLogRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getMetadata())
                .containsEntry("ip", "127.0.0.1")
                .containsEntry("success", true);
    }

    @Test
    @DisplayName("findByUser_IdOrderByCreatedAtDesc liefert Logs absteigend nach Zeit")
    void testFindByUserIdOrderByCreatedAtDesc() throws InterruptedException {
        User u = createUser();

        AuditLog log1 = AuditLog.builder()
                .user(u)
                .action("A")
                .metadata(Map.of())
                .build();
        AuditLog log2 = AuditLog.builder()
                .user(u)
                .action("B")
                .metadata(Map.of())
                .build();

        auditLogRepository.saveAndFlush(log1);
        // Kurze Pause, damit createdAt sich unterscheidet
        Thread.sleep(10);
        auditLogRepository.saveAndFlush(log2);

        List<AuditLog> logs = auditLogRepository.findByUser_IdOrderByCreatedAtDesc(u.getId());
        assertThat(logs)
                .hasSize(2)
                .extracting(AuditLog::getAction)
                .containsExactly("B", "A");
    }

    @Test
    @DisplayName("findByUser_IdOrderByCreatedAtDesc gibt leere Liste, wenn keine Logs")
    void testFindByUserIdNoLogs() {
        UUID randomId = UUID.randomUUID();
        List<AuditLog> logs = auditLogRepository.findByUser_IdOrderByCreatedAtDesc(randomId);
        assertThat(logs).isEmpty();
    }
}
