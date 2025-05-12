// src/main/java/com/elysion/user/exception/UserAlreadyExistsException.java
package com.elysion.user.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}