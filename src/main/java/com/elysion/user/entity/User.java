// src/main/java/com/elysion/user/entity/User.java
package com.elysion.user.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "USERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "PASSWORD_HASH", nullable = false)
    private String passwordHash;

    private String name;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean isActive = false;

    @Column(name = "IS_BANNED", nullable = false)
    private boolean isBanned = false;

    @Column(name = "DELETED_AT")
    private Instant deletedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "USER_ROLES",
            joinColumns = @JoinColumn(name = "USER_ID"),
            inverseJoinColumns = @JoinColumn(name = "ROLE_ID"))
    private Set<Role> roles;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
