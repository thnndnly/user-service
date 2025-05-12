// src/main/java/com/elysion/user/service/AuthService.java
package com.elysion.user.service;

import com.elysion.user.dto.LoginRequest;
import com.elysion.user.dto.LoginResponse;
import com.elysion.user.dto.RegisterRequest;
import com.elysion.user.dto.TwoFaSetupResponse;
import com.elysion.user.entity.*;
import com.elysion.user.exception.*;
import com.elysion.user.repository.*;
import com.elysion.user.config.JwtUtil;
import com.elysion.user.util.SecurityUtil;
import com.elysion.user.util.TotpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationTokenRepository emailTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final AuditLogService auditLogService;
    private final TwoFactorSecretRepository twoFactorSecretRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.refresh-token-expiration-ms}")
    private long refreshTokenDurationMs;

    @Transactional
    public void registerUser(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new UserAlreadyExistsException("Email already in use");
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setName(req.getName());
        user.setActive(false);
        user.setBanned(false);
        // Standard-Rolle "customer"
        if(user.getRoles() == null) user.setRoles(new java.util.HashSet<>());

        roleRepository.findByName("customer")
                .ifPresent(user.getRoles()::add);
        userRepository.save(user);

        // Double-Opt-In Token anlegen
        String token = UUID.randomUUID().toString();
        EmailVerificationToken emailToken = new EmailVerificationToken();
        emailToken.setUser(user);
        emailToken.setVerificationToken(token);
        emailToken.setExpiresAt(Instant.now().plusSeconds(3600)); // 1 h gültig
        emailTokenRepository.save(emailToken);

        emailService.sendVerificationEmail(user, token);
        auditLogService.logEvent(user.getId(), "REGISTER", Map.of("email", user.getEmail()));
    }

    @Transactional
    public void confirmEmail(String token) {
        EmailVerificationToken emailToken = emailTokenRepository.findByVerificationToken(token)
                .orElseThrow(() -> new TokenNotFoundException("Verification token not found"));
        if (emailToken.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Verification token expired");
        }
        User user = emailToken.getUser();
        user.setActive(true);
        userRepository.save(user);
        emailToken.setUsedAt(Instant.now());
        emailTokenRepository.save(emailToken);

        auditLogService.logEvent(user.getId(), "EMAIL_CONFIRMED", Map.of());
    }

    @Transactional
    public LoginResponse authenticate(LoginRequest req) {
        // 1) Credentials prüfen
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.isActive() || user.isBanned()) {
            throw new AccessDeniedException("User inactive or banned");
        }

        // 2) 2FA prüfen (falls aktiviert)
        twoFactorSecretRepository.findByUserId(user.getId())
                .filter(TwoFactorSecret::isEnabled)
                .ifPresent(secret -> {
                    String codeStr = req.getTotpCode();
                    if (codeStr == null) {
                        throw new TwoFactorAuthenticationException("2FA code required");
                    }
                    int code;
                    try {
                        code = Integer.parseInt(codeStr);
                    } catch (NumberFormatException e) {
                        throw new TwoFactorAuthenticationException("Invalid 2FA code format");
                    }
                    if (!TotpUtil.verifyCode(secret.getTotpSecret(), code)) {
                        throw new TwoFactorAuthenticationException("Invalid 2FA code");
                    }
                });

        // 3) Tokens generieren
        String accessToken = jwtUtil.generateToken(userDetails);

        // neues Refresh Token
        String refreshTokenPlain = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(refreshTokenPlain);
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshTokenRepository.save(refreshToken);

        auditLogService.logEvent(user.getId(), "LOGIN", Map.of("ip", req.getIpAddress()));

        return new LoginResponse(accessToken, refreshTokenPlain , "Bearer");
    }

    @Transactional
    public LoginResponse refreshToken(String token) {
        RefreshToken rt = refreshTokenRepository.findByTokenHash(token)
                .orElseThrow(() -> new TokenNotFoundException("Refresh token not found"));
        if (rt.getExpiresAt().isBefore(Instant.now()) || rt.getRevokedAt() != null) {
            throw new TokenExpiredException("Refresh token expired or revoked");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(rt.getUser().getEmail());
        String newAccessToken = jwtUtil.generateToken(userDetails);
        auditLogService.logEvent(rt.getUser().getId(), "REFRESH_TOKEN", Map.of());

        // Im Response-DTO Access- und Refresh-Token zurückgeben
        return new LoginResponse(newAccessToken, token, "Bearer");
    }

    /** Passwort-vergessen-Flow: Token generieren und Mail senden */
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        String token = UUID.randomUUID().toString();

        PasswordResetToken pr = new PasswordResetToken();
        pr.setUser(user);
        pr.setResetToken(token);
        pr.setExpiresAt(Instant.now().plusSeconds(3600)); // 1h
        passwordResetTokenRepository.save(pr);

        emailService.sendPasswordResetEmail(user, token);
        auditLogService.logEvent(user.getId(), "PASSWORD_RESET_REQUESTED", Map.of());
    }

    /** Passwort zurücksetzen mit Token */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken pr = passwordResetTokenRepository.findByResetToken(token)
                .orElseThrow(() -> new TokenNotFoundException("Reset token not found"));
        if (pr.getExpiresAt().isBefore(Instant.now()) || pr.getUsedAt() != null) {
            throw new TokenExpiredException("Reset token expired or already used");
        }
        User user = pr.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        pr.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(pr);

        auditLogService.logEvent(user.getId(), "PASSWORD_RESET", Map.of());
    }

    /** 2FA-Setup: Secret generieren und QR-Code-URL zurückliefern */
    @Transactional
    public TwoFaSetupResponse setupTwoFa() {
        // Aktuellen User holen
        String email = SecurityUtil.getCurrentUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String secret = TotpUtil.generateSecret();
        // Optional: otpauth URL für QR-Code
        String qrUrl = TotpUtil.getOtpAuthURL("Elysion", user.getEmail(), secret);

        TwoFactorSecret tfs = twoFactorSecretRepository.findByUserId(user.getId())
                .orElse(new TwoFactorSecret());
        tfs.setUser(user);
        tfs.setTotpSecret(secret);
        tfs.setEnabled(false);
        // recoveryCodes könnten hier generiert werden
        twoFactorSecretRepository.save(tfs);

        return new TwoFaSetupResponse(secret, qrUrl);
    }

    /** 2FA-Code verifizieren (nur Check, ohne Aktivierung) */
    @Transactional
    public void verifyTwoFa(String codeStr) {
        User user = userRepository.findByEmail(SecurityUtil.getCurrentUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        TwoFactorSecret tfs = twoFactorSecretRepository.findByUserId(user.getId())
                .orElseThrow(() -> new TwoFactorAuthenticationException("2FA not setup"));

        int code = TotpUtil.parseCode(codeStr);
        if (!TotpUtil.verifyCode(tfs.getTotpSecret(), code)) {
            throw new TwoFactorAuthenticationException("Invalid 2FA code");
        }
    }

    /** 2FA aktivieren nach erfolgreicher Verifikation */
    @Transactional
    public void enableTwoFa(String codeStr) {
        verifyTwoFa(codeStr);
        User user = userRepository.findByEmail(SecurityUtil.getCurrentUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        TwoFactorSecret tfs = twoFactorSecretRepository.findByUserId(user.getId()).get();
        tfs.setEnabled(true);
        twoFactorSecretRepository.save(tfs);
        auditLogService.logEvent(user.getId(), "2FA_ENABLED", Map.of());
    }

    /** 2FA deaktivieren nach Code-Verifikation */
    @Transactional
    public void disableTwoFa(String codeStr) {
        verifyTwoFa(codeStr);
        User user = userRepository.findByEmail(SecurityUtil.getCurrentUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        TwoFactorSecret tfs = twoFactorSecretRepository.findByUserId(user.getId()).get();
        tfs.setEnabled(false);
        twoFactorSecretRepository.save(tfs);
        auditLogService.logEvent(user.getId(), "2FA_DISABLED", Map.of());
    }

    @Transactional
    public void logout(String token) {
        refreshTokenRepository.findByTokenHash(token).ifPresent(rt -> {
            rt.setRevokedAt(Instant.now());
            refreshTokenRepository.save(rt);
            auditLogService.logEvent(rt.getUser().getId(), "LOGOUT", Map.of());
        });
    }
}
