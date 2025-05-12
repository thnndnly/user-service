package com.elysion.user.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility-Klasse zur zuverlässigen Ermittlung der Client-IP,
 * auch wenn die Anwendung hinter einem Proxy oder Load Balancer läuft.
 */
public class IpUtil {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP       = "X-Real-IP";

    /**
     * Liest die IP-Adresse des Clients aus dem Request.
     * Prüft zuerst gängige Proxy-Header, fällt sonst auf request.getRemoteAddr() zurück.
     *
     * @param request der HttpServletRequest
     * @return die ermittelte Client-IP
     */
    public static String getClientIp(HttpServletRequest request) {
        // 1. X-Forwarded-For (kann mehrere Einträge enthalten, oldest first)
        String xff = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xff != null && !xff.isBlank()) {
            // mehrere IPs: erste ist die ursprüngliche Client-IP
            return xff.split(",")[0].trim();
        }

        // 2. X-Real-IP
        String xrip = request.getHeader(HEADER_X_REAL_IP);
        if (xrip != null && !xrip.isBlank()) {
            return xrip.trim();
        }

        // 3. Fallback
        return request.getRemoteAddr();
    }
}