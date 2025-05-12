// src/main/java/com/elysion/user/exception/AccessDeniedException.java
package com.elysion.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Wird geworfen, wenn ein Nutzer keine ausreichenden Berechtigungen besitzt.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}