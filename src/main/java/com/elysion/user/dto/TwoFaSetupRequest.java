// src/main/java/com/elysion/user/dto/TwoFaSetupRequest.java
package com.elysion.user.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class TwoFaSetupRequest {
    /**
     * Das von der Anwendung generierte TOTP-Secret
     */
    @NotBlank
    private String secret;

    /**
     * Der erste von der Authenticator-App generierte Code zur Verifikation
     */
    @NotBlank
    private String code;
}
