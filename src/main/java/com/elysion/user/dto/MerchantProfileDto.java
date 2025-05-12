package com.elysion.user.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class MerchantProfileDto {
    private UUID userId;
    private String companyName;
    private String taxId;
    private String address;
    private String website;
    private boolean verified;
}
