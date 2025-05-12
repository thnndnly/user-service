// src/main/java/com/elysion/user/entity/LoginAttempt.java
package com.elysion.user.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "LOGIN_ATTEMPTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private User user;

    @Column(name = "IP_ADDRESS")
    private String ipAddress;

    @Column(name = "ATTEMPT_TIME", nullable = false)
    private Instant attemptTime;

    @Column(name = "WAS_SUCCESSFUL", nullable = false)
    private boolean wasSuccessful;

    @PrePersist
    public void prePersist() {
        this.attemptTime = Instant.now();
    }
}