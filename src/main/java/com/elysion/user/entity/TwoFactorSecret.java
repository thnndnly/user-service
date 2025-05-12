// src/main/java/com/elysion/user/entity/TwoFactorSecret.java
package com.elysion.user.entity;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "TWO_FACTOR_SECRETS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorSecret {
    @Id
    @Column(name = "USER_ID")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "USER_ID")
    private User user;

    @Column(name = "TOTP_SECRET", nullable = false)
    private String totpSecret;

    @Column(name = "IS_ENABLED", nullable = false)
    private boolean isEnabled = false;

    @JdbcTypeCode(SqlTypes.JSON)         // <- hier sagen wir Hibernate, dass es jsonb bindet
    @Column(name = "RECOVERY_CODES", columnDefinition = "jsonb")
    private String recoveryCodes;
}