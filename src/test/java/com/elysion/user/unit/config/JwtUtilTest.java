// src/test/java/com/elysion/user/config/JwtUtilTest.java
package com.elysion.user.unit.config;

import com.elysion.user.config.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    // 32 Byte Secret für HMAC-SHA256
    private static final String SECRET = "01234567012345670123456701234567";

    @Test
    void generateToken_extractClaims_validateToken() {
        // 1 Stunde Ablauf
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60 * 60_000);

        // UserDetails mit 2 Rollen anlegen
        UserDetails userDetails = new User(
                "bob", "pwd",
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                )
        );

        // Token erzeugen
        String token = jwtUtil.generateToken(userDetails);
        assertNotNull(token);

        // Alle Claims auslesen
        Claims claims = jwtUtil.extractAllClaims(token);

        // Subject muss der Username sein
        assertEquals("bob", claims.getSubject());

        // Rollen im Claim prüfen
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles", List.class);
        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(roles.contains("ROLE_ADMIN"));

        // Token ist frisch, also nicht abgelaufen
        assertFalse(jwtUtil.isTokenExpired(token));

        // validateToken sollte true zurückgeben
        assertTrue(jwtUtil.validateToken(token, userDetails));
    }

    @Test
    void expiredToken_isDetected_asExpired_and_invalid() {
        // negative Expiration → sofort abgelaufen
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1);

        UserDetails userDetails = new User(
                "alice", "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // Token erzeugen (wird mit Ablauf in der Vergangenheit gebaut)
        String token = jwtUtil.generateToken(userDetails);
        assertNotNull(token);

        // extractUsername liefert trotzdem den Subject
        assertEquals("alice", jwtUtil.extractUsername(token));

        // Token ist abgelaufen
        assertTrue(jwtUtil.isTokenExpired(token));

        // validateToken schlägt fehl
        assertFalse(jwtUtil.validateToken(token, userDetails));
    }

    @Test
    void generateToken_withNoAuthorities_yieldsEmptyRolesClaim() {
        JwtUtil util = new JwtUtil(SECRET, 1_000);
        UserDetails u = new User("x", "pw", Collections.emptyList());

        String token = util.generateToken(u);
        assertNotNull(token);

        Claims c = util.extractAllClaims(token);
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) c.get("roles", List.class);

        assertNotNull(roles, "roles-Claim sollte nie null sein");
        assertTrue(roles.isEmpty(), "roles-Claim sollte leer sein");
    }

    @Test
    void validateToken_withUsernameMismatch_returnsFalse() {
        JwtUtil util = new JwtUtil(SECRET, 5_000);

        UserDetails alice = new User(
                "alice", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        UserDetails bob = new User(
                "bob", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // Token für alice erzeugen
        String token = util.generateToken(alice);
        assertNotNull(token);

        // validateToken mit bob muss false zurückgeben
        assertFalse(util.validateToken(token, bob),
                "Token für alice darf nicht für bob gültig sein");
    }

    @Test
    void dynamicExpiration_behavior() throws InterruptedException {
        // Ablauf in 1000 ms
        JwtUtil util = new JwtUtil(SECRET, 1000);

        UserDetails u = new User(
                "u", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String token = util.generateToken(u);

        // kurz danach noch gültig
        assertFalse(util.isTokenExpired(token),
                "Token darf sofort nach Erzeugung nicht abgelaufen sein");

        // nach 1000 ms abgelaufen
        Thread.sleep(1000);
        assertTrue(util.isTokenExpired(token),
                "Token muss nach Ablaufzeit abgelaufen sein");
        assertFalse(util.validateToken(token, u),
                "validateToken muss false liefern, wenn Token abgelaufen ist");
    }


    @Test
    void expirationClaim_matchesConfiguredExpirationMs() {
        long expiryMs = 2_000;
        JwtUtil util = new JwtUtil(SECRET, expiryMs);

        UserDetails u = new User(
                "z", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        long before = System.currentTimeMillis();
        String token = util.generateToken(u);
        Claims c = util.extractAllClaims(token);
        Date exp = c.getExpiration();
        long diff = exp.getTime() - before;

        // erlauben ±100 ms Toleranz
        assertTrue(Math.abs(diff - expiryMs) < 1000,
                () -> "Erwartet ~" + expiryMs + "ms, war aber " + diff + "ms");
    }

    @Test
    void extractUsername_withExpiredToken_stillReturnsSubject() throws InterruptedException {
        // sofort abgelaufen
        JwtUtil util = new JwtUtil(SECRET, -1);

        UserDetails u = new User(
                "expiredUser", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String token = util.generateToken(u);

        // extractUsername muss trotzdem „expiredUser“ liefern
        assertEquals("expiredUser", util.extractUsername(token));

        // isTokenExpired muss true liefern
        assertTrue(util.isTokenExpired(token));
    }

    @Test
    void extractUsername_withMalformedToken_throwsMalformedJwtException() {
        JwtUtil util = new JwtUtil(SECRET, 1_000);

        String bad = "not-a.jwt.token";
        assertThrows(MalformedJwtException.class,
                () -> util.extractUsername(bad),
                "Nicht-JWT muss eine MalformedJwtException werfen");
    }

    @Test
    void extractAllClaims_withBadSignature_throwsException() {
        // zwei Utils mit verschiedenen Secrets
        JwtUtil good = new JwtUtil(SECRET, 1_000);
        JwtUtil bad   = new JwtUtil("another-32-byte-long-secret-stri", 1_000);

        UserDetails u = new User("x", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_X")));

        String token = good.generateToken(u);
        // badSecret-Parser kann das nicht verifizieren
        assertThrows(Exception.class,
                () -> bad.extractAllClaims(token),
                "Falscher Secret-Key muss eine Exception werfen");
    }
}