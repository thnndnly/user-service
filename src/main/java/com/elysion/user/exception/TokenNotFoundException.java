// src/main/java/com/elysion/user/exception/TokenNotFoundException.java
package com.elysion.user.exception;

public class TokenNotFoundException extends RuntimeException {
    public TokenNotFoundException(String message) {
        super(message);
    }
}