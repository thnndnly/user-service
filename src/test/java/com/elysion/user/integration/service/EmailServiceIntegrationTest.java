package com.elysion.user.integration.service;

import com.elysion.user.entity.User;
import com.elysion.user.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")   // lädt application-test.properties mit MailHog-Einstellungen
class EmailServiceIntegrationTest {

    @Autowired
    private EmailService emailService;

    @Test
    @DisplayName("sendVerificationEmail darf keine Exception werfen")
    void sendVerificationEmailShouldNotThrow() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");

        assertThatCode(() -> emailService.sendVerificationEmail(user, "token123"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendPasswordResetEmail darf keine Exception werfen")
    void sendPasswordResetEmailShouldNotThrow() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");

        assertThatCode(() -> emailService.sendPasswordResetEmail(user, "resetToken"))
                .doesNotThrowAnyException();
    }
}