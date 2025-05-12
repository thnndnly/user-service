// src/main/java/com/elysion/user/service/AuditLogService.java
package com.elysion.user.service;

import com.elysion.user.entity.AuditLog;
import com.elysion.user.entity.User;
import com.elysion.user.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @PersistenceContext
    private EntityManager em;

    /**
     * Legt einen Audit-Log-Eintrag an. Wir verwenden hier einen User-Stumpf (nur ID),
     * um das vollständige Laden der User-Entität zu vermeiden.
     */
    @Transactional
    public void logEvent(UUID userId, String action, Map<String, Object> metadata) {
        User userProxy = em.getReference(User.class, userId);

        // 2) AuditLog anlegen und speichern
        AuditLog log = AuditLog.builder()
                .user(userProxy)
                .action(action)
                .metadata(metadata)
                .build();
        auditLogRepository.save(log);
    }

    /**
     * Liefert alle Einträge für einen Nutzer sortiert nach Erstellungszeit (neueste zuerst).
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsForUser(UUID userId) {
        return auditLogRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }
}