package com.elysion.user.controller;

import com.elysion.user.config.JwtUtil;
import com.elysion.user.dto.*;
import com.elysion.user.entity.EmailVerificationToken;
import com.elysion.user.entity.PasswordResetToken;
import com.elysion.user.entity.RefreshToken;
import com.elysion.user.entity.User;
import com.elysion.user.repository.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired private UserRepository userRepository;
    @Autowired private EmailVerificationTokenRepository emailTokenRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String base = "/auth";

    @BeforeEach
    void cleanDb() {
        auditLogRepository.deleteAll();               // ← neu
        // Reihenfolge beachten wg. Foreign Keys
        refreshTokenRepository.deleteAll();
        emailTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /auth/register → 201 + User + VerificationToken")
    void register_createsUserAndToken() {
        var req = new RegisterRequest("neo@matrix.org","trinity","Neo", false, List.of());
        ResponseEntity<Void> resp = restTemplate.postForEntity(base + "/register", req, Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        User u = userRepository.findByEmail("neo@matrix.org").orElseThrow();
        assertThat(u.isActive()).isFalse();
        assertThat(passwordEncoder.matches("trinity", u.getPasswordHash())).isTrue();

        List<EmailVerificationToken> tokens = emailTokenRepository.findAll();
        assertThat(tokens).singleElement()
                .satisfies(t -> {
                    assertThat(t.getUser().getId()).isEqualTo(u.getId());
                    assertThat(t.getExpiresAt()).isAfter(Instant.now());
                });
    }

    @Test
    @DisplayName("GET /auth/confirm → 200 + User aktiviert + Token usedAt gesetzt")
    void confirmEmail_activatesUserAndMarksToken() {
        // erst registrieren
        var reg = new RegisterRequest("morpheus@matrix.org","zion","Morpheus", false, List.of());
        restTemplate.postForEntity(base + "/register", reg, Void.class);
        User u = userRepository.findByEmail("morpheus@matrix.org").orElseThrow();

        // Token ermitteln
        String token = emailTokenRepository.findAll().stream()
                .filter(t->t.getUser().getId().equals(u.getId()))
                .map(EmailVerificationToken::getVerificationToken)
                .findFirst().orElseThrow();

        // bestätigen
        ResponseEntity<Void> resp = restTemplate.getForEntity(base + "/confirm?token=" + token, Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        User reloaded = userRepository.findByEmail("morpheus@matrix.org").orElseThrow();
        assertThat(reloaded.isActive()).isTrue();

        EmailVerificationToken saved = emailTokenRepository.findByVerificationToken(token).orElseThrow();
        assertThat(saved.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("POST /auth/login → 200 + LoginResponse + RefreshToken persistiert")
    void login_returnsTokensAndPersistsRefreshToken() {
        // registrieren + bestätigen
        var reg = new RegisterRequest("trinity@matrix.org","neo","Trinity", false, List.of());
        restTemplate.postForEntity(base + "/register", reg, Void.class);
        User u = userRepository.findByEmail("trinity@matrix.org").orElseThrow();
        String vToken = emailTokenRepository.findAll().stream()
                .filter(t->t.getUser().getId().equals(u.getId()))
                .map(EmailVerificationToken::getVerificationToken)
                .findFirst().orElseThrow();
        restTemplate.getForEntity(base + "/confirm?token=" + vToken, Void.class);

        // login
        var loginReq = new LoginRequest("trinity@matrix.org","neo", null, "127.0.0.1");
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity(base + "/login", loginReq, LoginResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        LoginResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getAccessToken()).isNotBlank();
        assertThat(body.getRefreshToken()).isNotBlank();
        assertThat(body.getTokenType()).isEqualTo("Bearer");

        RefreshToken rt = refreshTokenRepository.findByTokenHash(body.getRefreshToken()).orElseThrow();
        assertThat(rt.getUser().getId()).isEqualTo(u.getId());
        assertThat(rt.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("POST /auth/refresh → 200 + neue AccessToken")
    void refresh_returnsNewAccessToken() {
        // login aus vorherigem Test nachbauen
        var reg = new RegisterRequest("neo2@matrix.org","pw","Neo2", false, List.of());
        restTemplate.postForEntity(base + "/register", reg, Void.class);
        User u = userRepository.findByEmail("neo2@matrix.org").orElseThrow();
        String vToken = emailTokenRepository.findAll().stream()
                .filter(t->t.getUser().getId().equals(u.getId()))
                .map(EmailVerificationToken::getVerificationToken)
                .findFirst().orElseThrow();
        restTemplate.getForEntity(base + "/confirm?token=" + vToken, Void.class);

        var loginReq = new LoginRequest("neo2@matrix.org","pw", null, "127.0.0.1");
        LoginResponse login = restTemplate.postForObject(base + "/login", loginReq, LoginResponse.class);

        // 1) Refresh-Request absenden
        var refreshReq = new RefreshTokenRequest(login.getRefreshToken());
        ResponseEntity<LoginResponse> resp =
                restTemplate.postForEntity(base + "/refresh", refreshReq, LoginResponse.class);

        // 2) HTTP 200
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 3) Body nicht null
        LoginResponse body = resp.getBody();
        assertThat(body).isNotNull();

        // 4) Access-Token ist nicht leer
        String newAccessToken = body.getAccessToken();
        assertThat(newAccessToken).isNotBlank();

        // 5) und stimmt vom Subject her überein
        assertThat(jwtUtil.extractUsername(newAccessToken))
                .isEqualTo("neo2@matrix.org");

        // 6) Der Refresh-Token bleibt unverändert
        assertThat(body.getRefreshToken())
                .isEqualTo(login.getRefreshToken());
    }

    @Test
    @DisplayName("POST /auth/forgot-password & /auth/reset-password")
    void forgotAndResetPassword_flow() {
        // registrieren + bestätigen
        var reg = new RegisterRequest("swap@matrix.org","orig","Swap", false, List.of());
        restTemplate.postForEntity(base + "/register", reg, Void.class);
        User u = userRepository.findByEmail("swap@matrix.org").orElseThrow();
        String vToken = emailTokenRepository.findAll().stream()
                .filter(t->t.getUser().getId().equals(u.getId()))
                .map(EmailVerificationToken::getVerificationToken)
                .findFirst().orElseThrow();
        restTemplate.getForEntity(base + "/confirm?token=" + vToken, Void.class);

        // forgot-password
        var forgotReq = new PasswordResetRequest("swap@matrix.org");
        ResponseEntity<Void> fResp = restTemplate.postForEntity(base + "/forgot-password", forgotReq, Void.class);
        assertThat(fResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        PasswordResetToken pr = passwordResetTokenRepository.findAll().stream()
                .filter(t->t.getUser().getId().equals(u.getId()))
                .findFirst().orElseThrow();
        assertThat(pr.getExpiresAt()).isAfter(Instant.now());

        // reset-password
        var resetReq = new ResetPasswordRequest(pr.getResetToken(), "newpass");
        ResponseEntity<Void> rResp = restTemplate.postForEntity(base + "/reset-password", resetReq, Void.class);
        assertThat(rResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        User reloaded = userRepository.findByEmail("swap@matrix.org").orElseThrow();
        assertThat(passwordEncoder.matches("newpass", reloaded.getPasswordHash())).isTrue();
    }
}