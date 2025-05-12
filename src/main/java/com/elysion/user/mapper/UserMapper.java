package com.elysion.user.mapper;

import com.elysion.user.dto.UserDto;
import com.elysion.user.entity.Role;
import com.elysion.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    /**
     * Wandelt ein User‐Entity in ein UserDto um.
     * Dabei werden die Rollen als Set&lt;String&gt; übergeben.
     */
    @Mapping(target = "roles", expression = "java(mapRoles(user.getRoles()))")
    UserDto toDto(User user);

    /**
     * Hilfsmethode: Extrahiert aus den Role‐Entities nur die Namen.
     */
    default Set<String> mapRoles(Set<Role> roles) {
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Wandelt ein UserDto in ein User‐Entity um.
     * Passwort‐Hash, Rollen und Audit‐Felder werden ignoriert,
     * sie werden z.B. in der Service‐Schicht gesetzt.
     */
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "roles",        ignore = true)
    @Mapping(target = "isActive",     ignore = true)
    @Mapping(target = "isBanned",     ignore = true)
    @Mapping(target = "deletedAt",    ignore = true)
    User toEntity(UserDto dto);
}