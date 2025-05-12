// src/main/java/com/elysion/user/entity/Role.java
package com.elysion.user.entity;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "ROLES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;
}
