package com.elysion.user.controller;

import com.elysion.user.dto.*;
import com.elysion.user.entity.EmailVerificationToken;
import com.elysion.user.repository.AuditLogRepository;
import com.elysion.user.repository.EmailVerificationTokenRepository;
import com.elysion.user.repository.RefreshTokenRepository;
import com.elysion.user.repository.UserRepository;
import com.elysion.user.service.AuthService;
import com.elysion.user.service.EmailService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")  // lädt application-test.properties
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserControllerIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository emailTokenRepository;

    @Autowired
    private EmailService emailService;  // verhindert echte Mail-Versendung

    private String bearerToken;

    @BeforeEach
    void setUp() {

        auditLogRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        emailTokenRepository.deleteAll();
        userRepository.deleteAll();

        // 1) User registrieren
        String email = "john@doe.org";
        String password = "secret";
        String name = "John Doe";
        authService.registerUser(new RegisterRequest(email, password, name, false, List.of()));

        var user = userRepository.findByEmail(email).orElseThrow();


        // 2) Token auslesen und bestätigen
        String token = emailTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .map(EmailVerificationToken::getVerificationToken)
                .findFirst()
                .orElseThrow();
        authService.confirmEmail(token);

        // 3) Login → JWT
        LoginResponse login = authService.authenticate(
                new LoginRequest(email, password, null, "127.0.0.1"));
        bearerToken = login.getTokenType() + " " + login.getAccessToken(); // z.B. "Bearer ey..."
    }

    @Test
    @DisplayName("GET /users/me returns current profile")
    void getProfile_returnsUserDto() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<UserDto> resp = restTemplate.exchange(
                "/users/me", HttpMethod.GET, req, UserDto.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).satisfies(u -> {
            assertThat(u.getEmail()).isEqualTo("john@doe.org");
            assertThat(u.getName()).isEqualTo("John Doe");
        });
    }

    @Test
    @DisplayName("PATCH /users/me updates profile")
    void updateProfile_updatesName() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        UserDto update = new UserDto();
        update.setName("Johnny");
        HttpEntity<UserDto> req = new HttpEntity<>(update, headers);

        ResponseEntity<UserDto> resp = restTemplate.exchange(
                "/users/me", HttpMethod.PATCH, req, UserDto.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(resp.getBody());
        assertThat(resp.getBody().getName()).isEqualTo("Johnny");

        // Gegencheck in der DB
        var user = userRepository.findByEmail("john@doe.org").orElseThrow();
        assertThat(user.getName()).isEqualTo("Johnny");
    }

    @Test
    @DisplayName("DELETE /users/me soft-deletes account")
    void deleteProfile_returnsNoContent() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<Void> resp = restTemplate.exchange(
                "/users/me", HttpMethod.DELETE, req, Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // In DB sollte deletedAt gesetzt und active=false sein
        var user = userRepository.findByEmail("john@doe.org").orElseThrow();
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("GET /users/me/history returns empty list")
    void getOrderHistory_returnsEmpty() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<OrderDto[]> resp = restTemplate.exchange(
                "/users/me/history", HttpMethod.GET, req, OrderDto[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEmpty();
    }

    @Test
    @DisplayName("GET /users/me/export returns JSON attachment")
    void exportData_returnsJsonAttachment() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<byte[]> resp = restTemplate.exchange(
                "/users/me/export", HttpMethod.GET, req, byte[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment; filename=\"user-data.json\"");
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

        Assertions.assertNotNull(resp.getBody());
        String json = new String(resp.getBody());
        assertThat(json).contains("\"email\"");
        assertThat(json).contains("John Doe");
    }
}