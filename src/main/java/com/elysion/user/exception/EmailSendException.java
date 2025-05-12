// src/main/java/com/elysion/user/exception/EmailSendException.java
package com.elysion.user.exception;

public class EmailSendException extends RuntimeException {
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}