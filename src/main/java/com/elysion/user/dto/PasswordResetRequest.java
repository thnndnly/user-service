// src/main/java/com/elysion/user/dto/PasswordResetRequest.java
package com.elysion.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {
    /**
     * E-Mail-Adresse, an die der Reset-Link gesendet wird
     */
    @NotBlank
    @Email
    private String email;
}