// src/main/java/com/elysion/user/dto/TwoFaSetupResponse.java
package com.elysion.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwoFaSetupResponse {
    /**
     * Das generierte TOTP-Secret (Base32)
     */
    private String secret;

    /**
     * URL für das QR-Code-Image (z.B. otpauth://...),
     * die der Client in einen QR-Code-Generator ziehen kann.
     */
    private String qrCodeUrl;
}