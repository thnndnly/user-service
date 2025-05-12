// src/main/java/com/elysion/user/service/UserService.java
package com.elysion.user.service;

import com.elysion.user.dto.OrderDto;
import com.elysion.user.dto.UserDto;
import com.elysion.user.entity.User;
import com.elysion.user.exception.UserNotFoundException;
import com.elysion.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;   // neu injiziert

    public UserDto getCurrentUserProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return UserDto.fromEntity(u);
    }

    @Transactional
    public UserDto updateCurrentUserProfile(UserDto dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        u.setName(dto.getName());
        userRepository.save(u);
        auditLogService.logEvent(u.getId(), "PROFILE_UPDATE", Map.of());
        return UserDto.fromEntity(u);
    }

    @Transactional
    public void deleteCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        u.setDeletedAt(Instant.now());
        u.setActive(false);
        userRepository.save(u);
        auditLogService.logEvent(u.getId(), "ACCOUNT_DELETED", Map.of());
    }

    /**
     * DSGVO-Datenexport: serialisiert alle Nutzerdaten und Audit-Logs als JSON.
     */
    @Transactional(readOnly = true)
    public byte[] exportCurrentUserData() {
        // 1) Nutzer aus SecurityContext ermitteln
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // 2) Basisdaten + Logs zusammensetzen
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("user", UserDto.fromEntity(u));
        data.put("auditLogs", auditLogService.getLogsForUser(u.getId()));

        auditLogService.logEvent(u.getId(), "DATA_EXPORTED", Map.of());

        // 3) JSON serialisieren
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Fehler beim Serialisieren der Nutzerdaten", e);
        }
    }

    public List<OrderDto> getOrderHistory() {
        // Placeholder: hier später Order-Daten aus deinem Order-Service laden
        return new ArrayList<>();
    }
}