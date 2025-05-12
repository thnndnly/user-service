package com.elysion.user.controller;

import com.elysion.user.dto.AuditLogEntryDto;
import com.elysion.user.dto.MerchantProfileDto;
import com.elysion.user.dto.RoleChangeRequest;
import com.elysion.user.dto.UserDto;
import com.elysion.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public List<UserDto> listUsers() {
        return userService.getAllUsers();
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<Void> changeRole(@PathVariable UUID id, @RequestBody RoleChangeRequest req) {
        userService.updateUserRole(id, req.getRole());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/{id}/ban")
    public ResponseEntity<Void> banUser(@PathVariable UUID id) {
        userService.setBannedStatus(id, true);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/{id}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable UUID id) {
        userService.setBannedStatus(id, false);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/merchants/{id}")
    public ResponseEntity<MerchantProfileDto> getMerchantProfile(@PathVariable UUID id) {
        MerchantProfileDto profile = userService.getMerchantProfile(id);
        return ResponseEntity.ok(profile);
    }

    @PatchMapping("/merchants/{id}/verify")
    public ResponseEntity<Void> verifyMerchant(@PathVariable UUID id) {
        userService.verifyMerchant(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}/audit-log")
    public ResponseEntity<List<AuditLogEntryDto>> getAuditLog(@PathVariable UUID id) {
        var logs = userService.getAuditLogForUser(id);
        return ResponseEntity.ok(logs);
    }
}
