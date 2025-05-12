// src/main/java/com/elysion/user/dto/LoginResponse.java
package com.elysion.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    /**
     * JWT Access Token
     */
    private String accessToken;

    /**
     * Refresh-Token zum Nachladen eines neuen Access Tokens
     */
    private String refreshToken;

    /**
     * Typ (meist "Bearer")
     */
    private String tokenType = "Bearer";
}