package com.elysion.user.integration.repository;

import com.elysion.user.entity.Role;
import com.elysion.user.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")  // lädt application-test.properties
@AutoConfigureTestDatabase(replace = Replace.NONE) // keine Embedded-DB
class RoleRepositoryIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void saveAndFindById_shouldPersistAndRetrieveRole() {
        Role role = Role.builder()
                .name("ROLE_TEST")
                .build();

        Role saved = roleRepository.save(role);
        assertThat(saved.getId()).isNotNull();

        Optional<Role> found = roleRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("ROLE_TEST");
    }

    @Test
    void findByName_existingRole_returnsRole() {
        Role role = Role.builder()
                .name("ROLE_ADMIN")
                .build();
        roleRepository.saveAndFlush(role);

        Optional<Role> byName = roleRepository.findByName("ROLE_ADMIN");
        assertThat(byName).isPresent();
        assertThat(byName.get().getName()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void save_duplicateName_throwsDataIntegrityViolation() {
        Role first = Role.builder()
                .name("DUPLICATE")
                .build();
        roleRepository.saveAndFlush(first);

        Role second = Role.builder()
                .name("DUPLICATE")
                .build();

        assertThatThrownBy(() -> roleRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}