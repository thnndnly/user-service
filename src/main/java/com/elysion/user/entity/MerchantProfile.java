// src/main/java/com/elysion/user/entity/MerchantProfile.java
package com.elysion.user.entity;

import lombok.*;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "MERCHANT_PROFILES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantProfile {
    @Id
    @Column(name = "USER_ID")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "USER_ID")
    private User user;

    @Column(name = "COMPANY_NAME")
    private String companyName;

    @Column(name = "TAX_ID")
    private String taxId;

    private String address;

    private String website;

    @Column(nullable = false)
    private boolean verified = false;
}