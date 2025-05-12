// AuditLogRepository.java
package com.elysion.user.repository;

import com.elysion.user.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByUser_IdOrderByCreatedAtDesc(UUID userId);
}