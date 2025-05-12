package com.elysion.user.util;

import com.elysion.user.exception.TwoFactorAuthenticationException;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

/**
 * Hilfsklasse für TOTP-basiertes 2FA (Time-Based One-Time Password).
 * Nutzt die Bibliothek com.warrenstrange:googleauth.
 */
public class TotpUtil {

    private static final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    /**
     * Erzeugt ein neues TOTP-Secret.
     * @return das Base32-kodierte Secret
     */
    public static String generateSecret() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    /**
     * Überprüft einen vom Nutzer eingegebenen TOTP-Code gegen das Secret.
     * @param secret das Base32-kodierte Secret
     * @param code   der 6-stellige TOTP-Code
     * @return true, wenn der Code gültig ist
     */
    public static boolean verifyCode(String secret, int code) {
        return gAuth.authorize(secret, code);
    }

    /**
     * Erzeugt die otpauth-URL für einen QR-Code, den man in einer Authenticator-App
     * scannen kann.
     *
     * @param issuer      Name deiner App/Organisation (z.B. "Elysion UG")
     * @param accountName Nutzerkennung/E-Mail, die in der App angezeigt wird
     * @param secret      das Base32-kodierte Secret
     * @return otpauth-URL für QR-Code-Generierung
     */
    public static String getOtpAuthURL(String issuer, String accountName, String secret) {
        // Aus dem String-Secret ein GoogleAuthenticatorKey bauen
        GoogleAuthenticatorKey key = new GoogleAuthenticatorKey.Builder(secret).build();
        // oder, falls deine Version der Bibliothek nur Builder().setKey(...) unterstützt:
        // GoogleAuthenticatorKey key = new GoogleAuthenticatorKey.Builder().setKey(secret).build();

        // Dann die URL generieren
        return GoogleAuthenticatorQRGenerator.getOtpAuthURL(issuer, accountName, key);
    }

    /** String → int mit Validierung */
    public static int parseCode(String codeStr) {
        try {
            return Integer.parseInt(codeStr);
        } catch (NumberFormatException e) {
            throw new TwoFactorAuthenticationException("Invalid 2FA code format");
        }
    }
}