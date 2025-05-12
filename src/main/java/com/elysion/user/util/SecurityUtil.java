// src/main/java/com/elysion/user/util/SecurityUtil.java
package com.elysion.user.util;

import com.elysion.user.exception.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    /**
     * Gibt den Usernamen (Subject) des aktuell authentifizierten Nutzers zurück.
     * Wirft eine AccessDeniedException, wenn kein Nutzer angemeldet ist.
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth.getPrincipal().equals("anonymousUser")) {
            throw new AccessDeniedException("Kein authentifizierter Nutzer gefunden");
        }
        return auth.getName();
    }
}