// src/main/java/com/elysion/user/dto/TwoFaVerifyRequest.java
package com.elysion.user.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class TwoFaVerifyRequest {
    /** 6-stelliger TOTP-Code */
    @NotBlank
    private String code;
}