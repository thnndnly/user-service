package com.elysion.user.integration.service;

import com.elysion.user.entity.AuditLog;
import com.elysion.user.entity.User;
import com.elysion.user.repository.AuditLogRepository;
import com.elysion.user.repository.UserRepository;
import com.elysion.user.service.AuditLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")                                // lädt src/test/resources/application-test.properties
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditLogService.class)                         // lädt dazu noch deinen Service
class AuditLogServiceIntegrationTest {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("logEvent should persist an AuditLog with correct fields")
    void logEventPersistsEntry() {
        // 1) echten User in die DB schreiben
        User user = new User();
        user.setEmail("foo@example.com");
        user.setPasswordHash("irrelevant");
        user.setName("Foo Bar");
        user.setActive(true);
        user.setBanned(false);
        user.setRoles(new HashSet<>());      // leere Rollen-Menge
        user = userRepository.save(user);

        UUID userId = user.getId();
        String action = "TEST_ACTION";
        Map<String,Object> meta = Map.of("foo","bar");

        auditLogService.logEvent(userId, action, meta);

        List<AuditLog> all = auditLogRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        assertThat(all).hasSize(1);

        AuditLog log = all.get(0);
        assertThat(log.getUser().getId()).isEqualTo(userId);
        assertThat(log.getAction()).isEqualTo(action);
        assertThat(log.getMetadata()).isEqualTo(meta);
        assertThat(log.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getLogsForUser should return entries in descending order")
    void getLogsForUserOrdersDescending() throws InterruptedException {
        // 1) echten User in die DB schreiben
        User user = new User();
        user.setEmail("foo@example.com");
        user.setPasswordHash("irrelevant");
        user.setName("Foo Bar");
        user.setActive(true);
        user.setBanned(false);
        user.setRoles(new HashSet<>());      // leere Rollen-Menge
        user = userRepository.save(user);

        // lege drei Einträge an
        auditLogService.logEvent(user.getId(), "A1", Map.of());
        Thread.sleep(5);
        auditLogService.logEvent(user.getId(), "A2", Map.of());
        Thread.sleep(5);
        auditLogService.logEvent(user.getId(), "A3", Map.of());

        // abrufen
        List<AuditLog> logs = auditLogService.getLogsForUser(user.getId());
        assertThat(logs).extracting(AuditLog::getAction)
                .containsExactly("A3", "A2", "A1");
    }
}