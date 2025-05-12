// src/main/java/com/elysion/user/entity/AuditLog.java
package com.elysion.user.entity;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "AUDIT_LOGS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Column(nullable = false)
    private String action;

    /**
     * Speichert JSON in einer jsonb-Spalte.
     */
    @Convert(converter = com.elysion.user.util.JpaJsonConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)         // <- hier sagen wir Hibernate, dass es jsonb bindet
    @Column(columnDefinition = "JSONB")
    private Map<String, Object> metadata;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}