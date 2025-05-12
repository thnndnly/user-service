// src/main/java/com/elysion/user/entity/RefreshToken.java
package com.elysion.user.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "REFRESH_TOKENS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Column(name = "TOKEN_HASH", nullable = false)
    private String tokenHash;

    @Column(name = "EXPIRES_AT", nullable = false)
    private Instant expiresAt;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "REVOKED_AT")
    private Instant revokedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}