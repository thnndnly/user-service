// src/main/java/com/elysion/user/exception/TwoFactorAuthenticationException.java
package com.elysion.user.exception;

public class TwoFactorAuthenticationException extends RuntimeException {
    public TwoFactorAuthenticationException(String message) {
        super(message);
    }
}