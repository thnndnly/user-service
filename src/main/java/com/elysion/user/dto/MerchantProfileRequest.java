package com.elysion.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantProfileRequest {
    private String companyName;
    private String taxId;
    private String address;
    private String website;
}
