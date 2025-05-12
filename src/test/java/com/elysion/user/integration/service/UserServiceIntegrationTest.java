package com.elysion.user.integration.service;

import com.elysion.user.dto.OrderDto;
import com.elysion.user.dto.UserDto;
import com.elysion.user.entity.AuditLog;
import com.elysion.user.entity.User;
import com.elysion.user.repository.AuditLogRepository;
import com.elysion.user.repository.UserRepository;
import com.elysion.user.service.AuditLogService;
import com.elysion.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest                              // <--- lädt den ganzen Kontext inkl. Services
@ActiveProfiles("test")                  // application-test.properties
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional                           // Rollback nach jedem Test
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    /** Hilfsmethode: legt einen User an und setzt ihn als "eingeloggt" */
    private User createAndAuthenticateUser() {
        User u = new User();
        u.setEmail("junit@example.org");
        u.setPasswordHash("irrelevant");
        u.setName("JUnit User");
        u.setActive(true);
        u.setBanned(false);
        u.setRoles(new HashSet<>());          // keine Rollen nötig
        u = userRepository.save(u);

        // SecurityContext mit UsernamePasswordAuthenticationToken befüllen
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u.getEmail(), null)
        );
        return u;
    }

    @Test
    @DisplayName("getCurrentUserProfile liefert korrekten UserDto")
    void getCurrentUserProfile() {
        User u = createAndAuthenticateUser();
        UserDto dto = userService.getCurrentUserProfile();

        assertThat(dto.getEmail()).isEqualTo(u.getEmail());
        assertThat(dto.getName()).isEqualTo(u.getName());
    }

    @Test
    @DisplayName("updateCurrentUserProfile ändert Name und schreibt AuditLog")
    void updateCurrentUserProfile() {
        User u = createAndAuthenticateUser();

        UserDto update = new UserDto();
        update.setName("Neuer Name");

        UserDto result = userService.updateCurrentUserProfile(update);
        assertThat(result.getName()).isEqualTo("Neuer Name");

        User reloaded = userRepository.findByEmail(u.getEmail()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Neuer Name");

        List<AuditLog> logs = auditLogRepository.findByUser_IdOrderByCreatedAtDesc(u.getId());
        assertThat(logs)
                .extracting(AuditLog::getAction)
                .contains("PROFILE_UPDATE");
    }

    @Test
    @DisplayName("deleteCurrentUser markiert gelöscht, deaktiviert und loggt")
    void deleteCurrentUser() {
        User u = createAndAuthenticateUser();

        userService.deleteCurrentUser();

        User reloaded = userRepository.findByEmail(u.getEmail()).orElseThrow();
        assertThat(reloaded.isActive()).isFalse();
        assertThat(reloaded.getDeletedAt()).isNotNull();

        List<AuditLog> logs = auditLogRepository.findByUser_IdOrderByCreatedAtDesc(u.getId());
        assertThat(logs)
                .extracting(AuditLog::getAction)
                .contains("ACCOUNT_DELETED");
    }

    @Test
    @DisplayName("exportCurrentUserData enthält User und AuditLogs, und loggt DATA_EXPORTED")
    void exportCurrentUserData() throws Exception {
        User u = createAndAuthenticateUser();
        // einen Test-Log anlegen
        auditLogService.logEvent(u.getId(), "TEST_LOG", Map.of("foo","bar"));

        byte[] jsonBytes = userService.exportCurrentUserData();
        String json = new String(jsonBytes, StandardCharsets.UTF_8);

        // Basischecks auf JSON-Inhalt
        assertThat(json).contains("\"user\"");
        assertThat(json).contains("\"email\":\"" + u.getEmail() + "\"");
        assertThat(json).contains("\"auditLogs\"");
        assertThat(json).contains("TEST_LOG");

        // Und: nach Export wurde ein DATA_EXPORTED-Log geschrieben
        List<AuditLog> logs = auditLogRepository.findByUser_IdOrderByCreatedAtDesc(u.getId());
        assertThat(logs.get(0).getAction()).isEqualTo("DATA_EXPORTED");
    }

    @Test
    @DisplayName("getOrderHistory liefert leere Liste")
    void getOrderHistoryEmpty() {
        createAndAuthenticateUser();
        List<OrderDto> orders = userService.getOrderHistory();
        assertThat(orders).isEmpty();
    }
}