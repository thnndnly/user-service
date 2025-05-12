// src/main/java/com/elysion/user/dto/UserDto.java
package com.elysion.user.dto;

import com.elysion.user.entity.Role;
import com.elysion.user.entity.User;
import lombok.Data;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
public class UserDto {
    private UUID id;
    private String email;
    private String name;

    /**
     * Rollen-Namen, z. B. ["ROLE_CUSTOMER", "ROLE_MERCHANT"]
     */
    private Set<String> roles;

    private boolean isActive;
    private boolean isBanned;
    private Instant createdAt;

    /**
     * Mappt eine User-Entität auf das DTO.
     */
    public static UserDto fromEntity(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setRoles(
                user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
        );
        dto.setActive(user.isActive());
        dto.setBanned(user.isBanned());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
