// src/main/java/com/elysion/user/controller/UserController.java
package com.elysion.user.controller;

import com.elysion.user.config.JwtUtil;
import com.elysion.user.dto.MerchantProfileRequest;
import com.elysion.user.dto.OrderDto;
import com.elysion.user.dto.UserDto;
import com.elysion.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil; // oder SecurityContext-Auslese

    /** Aktuelles Benutzerprofil abfragen */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getProfile() {
        UserDto user = userService.getCurrentUserProfile();
        return ResponseEntity.ok(user);
    }

    /** Eigenes Profil aktualisieren */
    @PatchMapping("/me")
    public ResponseEntity<UserDto> updateProfile(@RequestBody UserDto dto) {
        UserDto updated = userService.updateCurrentUserProfile(dto);
        return ResponseEntity.ok(updated);
    }

    /** Eigenes Konto löschen (Soft Delete) */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteProfile() {
        userService.deleteCurrentUser();
        return ResponseEntity.noContent().build();
    }

    /** Bestellhistorie des aktuellen Nutzers */
    @GetMapping("/me/history")
    public ResponseEntity<List<OrderDto>> getOrderHistory() {
        List<OrderDto> history = userService.getOrderHistory();
        return ResponseEntity.ok(history);
    }

    /** DSGVO-Datenexport als JSON-Download */
    @GetMapping(value = "/me/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> exportData() {
        byte[] data = userService.exportCurrentUserData();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"user-data.json\"")
                .body(data);
    }


    @PostMapping("/merchant-profile")
    public ResponseEntity<Void> updateMerchantProfile(
            @RequestBody MerchantProfileRequest req,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7); // Strip "Bearer "
        String email = jwtUtil.extractUsername(token);
        UUID userId = userService.getAllUsers().stream().filter(u -> u.getEmail().equals(email)).findFirst().get().getId();

        userService.createOrUpdateMerchantProfile(userId, req);
        return ResponseEntity.ok().build();
    }
}