// src/main/java/com/elysion/user/service/EmailService.java
package com.elysion.user.service;

import com.elysion.user.entity.User;
import com.elysion.user.exception.EmailSendException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public void sendVerificationEmail(User user, String token) {
        String link = frontendBaseUrl + "/auth/confirm?token=" + token;
        String subject = "Elysion: Bitte bestätige deine E-Mail-Adresse";
        String content = "Hallo " + user.getName() + ",\n\n"
                + "bitte bestätige dein Konto hier:\n" + link + "\n\n"
                + "Wenn du dich nicht registriert hast, ignoriere diese Mail.";
        sendEmail(user.getEmail(), subject, content);
    }

    public void sendPasswordResetEmail(User user, String token) {
        String link = frontendBaseUrl + "/auth/reset-password?token=" + token;
        String subject = "Elysion: Passwort zurücksetzen";
        String content = "Hallo " + user.getName() + ",\n\n"
                + "klicke hier, um dein Passwort zurückzusetzen:\n" + link
                + "\n\n(Dieser Link ist 1 Stunde gültig.)";
        sendEmail(user.getEmail(), subject, content);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
            h.setFrom(new InternetAddress(fromAddress, "Elysion Support"));
            h.setTo(to);
            h.setSubject(subject);
            h.setText(text, false);
            mailSender.send(msg);
        } catch (Exception e) {
            throw new EmailSendException("Failed to send email", e);
        }
    }
}