// src/main/java/com/elysion/user/service/UserService.java
package com.elysion.user.service;

import com.elysion.user.dto.*;
import com.elysion.user.entity.MerchantProfile;
import com.elysion.user.entity.Role;
import com.elysion.user.entity.User;
import com.elysion.user.exception.UserNotFoundException;
import com.elysion.user.repository.AuditLogRepository;
import com.elysion.user.repository.MerchantProfileRepository;
import com.elysion.user.repository.RoleRepository;
import com.elysion.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;   // neu injiziert
    private final RoleRepository roleRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    public UserDto getCurrentUserProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return UserDto.fromEntity(u);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(user -> {
            UserDto dto = new UserDto();
            dto.setId(user.getId());
            dto.setEmail(user.getEmail());
            dto.setName(user.getName());
            dto.setActive(user.isActive());
            dto.setBanned(user.isBanned());
            dto.setRoles(user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet()));
            return dto;
        }).toList();
    }

    public void updateUserRole(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Role newRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleName));

        // Optional: alle bisherigen Rollen entfernen
        user.getRoles().clear();

        // Neue Rolle hinzufügen
        user.getRoles().add(newRole);

        userRepository.save(user);

        auditLogService.logEvent(user.getId(), "ROLE_ASSIGNED", Map.of("role", roleName));
    }

    public void setBannedStatus(UUID userId, boolean banned) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        user.setBanned(banned);
    }

    public MerchantProfileDto getMerchantProfile(UUID userId) {
        MerchantProfile merchant = merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("Merchant profile not found"));

        MerchantProfileDto dto = new MerchantProfileDto();
        dto.setUserId(merchant.getUserId());
        dto.setCompanyName(merchant.getCompanyName());
        dto.setTaxId(merchant.getTaxId());
        dto.setAddress(merchant.getAddress());
        dto.setWebsite(merchant.getWebsite());
        dto.setVerified(merchant.isVerified());

        return dto;
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

    @Transactional
    public void createOrUpdateMerchantProfile(UUID userId, MerchantProfileRequest req) {
        MerchantProfile profile = merchantProfileRepository.findByUserId(userId)
                .orElseGet(MerchantProfile::new);

        profile.setUserId(userId);
        profile.setCompanyName(req.getCompanyName());
        profile.setTaxId(req.getTaxId());
        profile.setAddress(req.getAddress());
        profile.setWebsite(req.getWebsite());

        // Bei Update → Verifizierung ggf. zurücksetzen
        profile.setVerified(false);

        // Verknüpfter User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        profile.setUser(user);

        merchantProfileRepository.save(profile);

        auditLogService.logEvent(userId, "MERCHANT_PROFILE_UPDATED", Map.of(
                "company", req.getCompanyName()
        ));
    }

    @Transactional
    public void verifyMerchant(UUID userId) {
        MerchantProfile profile = merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("Merchant profile not found"));

        profile.setVerified(true);
        merchantProfileRepository.save(profile);

        auditLogService.logEvent(userId, "MERCHANT_VERIFIED", Map.of(
                "verifiedBy", "admin"
        ));
    }

    public List<AuditLogEntryDto> getAuditLogForUser(UUID userId) {
        return auditLogService.getLogsForUser(userId)
                .stream()
                .map(entry -> {
                    AuditLogEntryDto dto = new AuditLogEntryDto();
                    dto.setAction(entry.getAction());
                    dto.setTimestamp(entry.getCreatedAt());
                    dto.setMetadata(entry.getMetadata());
                    return dto;
                })
                .toList();
    }
}