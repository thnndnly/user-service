package com.elysion.user.integration.service;

import com.elysion.user.dto.LoginRequest;
import com.elysion.user.dto.LoginResponse;
import com.elysion.user.dto.RegisterRequest;
import com.elysion.user.entity.EmailVerificationToken;
import com.elysion.user.entity.RefreshToken;
import com.elysion.user.entity.User;
import com.elysion.user.exception.AccessDeniedException;
import com.elysion.user.exception.UserNotFoundException;
import com.elysion.user.repository.EmailVerificationTokenRepository;
import com.elysion.user.repository.RefreshTokenRepository;
import com.elysion.user.repository.UserRepository;
import com.elysion.user.service.AuthService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest                           // lädt den vollen Kontext inklusive Security, JwtUtil, EmailService…
@ActiveProfiles("test")                  // application-test.properties (Test-DB, jwt.secret, app.mail.from…)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional                           // Rollback nach jedem Test
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository emailTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("registerUser should create a new inactive user with a verification token")
    void registerUserCreatesUserAndToken() {
        // given
        var registerReq = new RegisterRequest("neo@matrix.org", "trinity", "Neo", false, List.of());

        // when
        authService.registerUser(registerReq);

        // then
        User user = userRepository.findByEmail("neo@matrix.org")
                .orElseThrow(() -> new AssertionError("User not persisted"));
        assertThat(user.isActive()).isFalse();
        assertThat(passwordEncoder.matches("trinity", user.getPasswordHash())).isTrue();
        assertThat(user.getName()).isEqualTo("Neo");

        EmailVerificationToken token = emailTokenRepository
                .findByVerificationToken(
                        emailTokenRepository.findAll().stream()
                                .filter(t->t.getUser().getId().equals(user.getId()))
                                .findFirst()
                                .orElseThrow().getVerificationToken()
                )
                .orElseThrow();
        assertThat(token.getUser().getId()).isEqualTo(user.getId());
        var before = Instant.now();
        assertThat(token.getExpiresAt()).isAfter(before);
    }

    @Test
    @DisplayName("confirmEmail should activate user and mark token used")
    void confirmEmailActivatesUser() {
        // Vorarbeit: anlegen wie oben
        var reg = new RegisterRequest("morpheus@matrix.org", "zion", "Morpheus", false, List.of());
        authService.registerUser(reg);
        User user = userRepository.findByEmail("morpheus@matrix.org")
                .orElseThrow();

        // again, token via findAll()
        String tokenValue = emailTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .map(EmailVerificationToken::getVerificationToken)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Verification token missing"));

        // when
        authService.confirmEmail(tokenValue);

        // then
        User reloaded = userRepository.findByEmail("morpheus@matrix.org")
                .orElseThrow();
        assertThat(reloaded.isActive()).isTrue();

        var savedToken = emailTokenRepository.findByVerificationToken(tokenValue)
                .orElseThrow();
        assertThat(savedToken.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("authenticate should return tokens and persist a refresh token")
    void authenticateReturnsTokensAndPersistsRefreshToken() {
        // Vorarbeit: registrieren + bestätigen
        var reg = new RegisterRequest("trinity@matrix.org", "neo", "Trinity", false, List.of());
        authService.registerUser(reg);
        User user = userRepository.findByEmail("trinity@matrix.org")
                .orElseThrow();
        String vToken = emailTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .map(EmailVerificationToken::getVerificationToken)
                .findFirst()
                .orElseThrow();
        authService.confirmEmail(vToken);

        // when
        var loginReq = new LoginRequest("trinity@matrix.org", "neo", null, "127.0.0.1");
        LoginResponse resp = authService.authenticate(loginReq);

        // then
        assertThat(resp.getAccessToken()).isNotBlank();
        assertThat(resp.getRefreshToken()).isNotBlank();
        assertThat(resp.getTokenType()).isEqualTo("Bearer");

        RefreshToken stored = refreshTokenRepository.findByTokenHash(resp.getRefreshToken())
                .orElseThrow();
        assertThat(stored.getUser().getId()).isEqualTo(user.getId());
        var before = Instant.now();
        assertThat(stored.getExpiresAt()).isAfter(before);
    }

    @Test
    @DisplayName("authenticate with inactive user should be denied")
    void authenticateInactiveShouldThrow() {
        var reg = new RegisterRequest("apoc@matrix.org", "oracle", "Apoc", false, List.of());
        authService.registerUser(reg);
        // nicht bestätigt = inactive

        var loginReq = new LoginRequest("apoc@matrix.org", "oracle", null, "127.0.0.1");
        assertThatThrownBy(() -> authService.authenticate(loginReq))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User inactive or banned");
    }
}