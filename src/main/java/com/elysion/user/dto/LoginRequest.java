// src/main/java/com/elysion/user/dto/LoginRequest.java
package com.elysion.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    /**
     * Optional: TOTP-Code, falls 2FA aktiviert ist
     */
    private String totpCode;

    /** Client-IP (müsste im Request-Body mitgesendet werden) */
    @NotBlank
    private String ipAddress;
}
