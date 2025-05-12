// src/main/java/com/elysion/user/dto/RegisterRequest.java
package com.elysion.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "Das Passwort muss mindestens 8 Zeichen lang sein")
    private String password;

    @NotBlank
    private String name;

    /**
     * Kennzeichnet, ob der Nutzer direkt als Händler registrieren möchte
     */
    private boolean requestMerchant;

    /**
     * Optionale Interessen (z. B. für personalisierte Empfehlungen)
     */
    private List<String> interests;
}
