package com.libseat.repository;

import com.libseat.entity.User;
import com.libseat.entity.UserRole;
import com.libseat.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends RepositoryTestBase {

    @Autowired
    private UserRepository userRepository;

    private User student;
    private User admin;

    @BeforeEach
    void setUp() {
        student = userRepository.save(user("S001", "wang@test.com", UserRole.STUDENT, UserStatus.ACTIVE));
        admin   = userRepository.save(user("A001", "admin@test.com", UserRole.ADMIN, UserStatus.ACTIVE));
        userRepository.save(user("S002", "li@test.com", UserRole.STUDENT, UserStatus.INACTIVE));
    }

    @Test
    void findByUserNo_existingNo_returnsUser() {
        assertThat(userRepository.findByUserNo("S001"))
                .isPresent()
                .hasValueSatisfying(u -> assertThat(u.getEmail()).isEqualTo("wang@test.com"));
    }

    @Test
    void findByUserNo_unknownNo_returnsEmpty() {
        assertThat(userRepository.findByUserNo("NONE")).isEmpty();
    }

    @Test
    void findByEmail_existingEmail_returnsUser() {
        assertThat(userRepository.findByEmail("admin@test.com"))
                .isPresent()
                .hasValueSatisfying(u -> assertThat(u.getRole()).isEqualTo(UserRole.ADMIN));
    }

    @Test
    void existsByEmail_existing_returnsTrue() {
        assertThat(userRepository.existsByEmail("wang@test.com")).isTrue();
    }

    @Test
    void existsByEmail_unknown_returnsFalse() {
        assertThat(userRepository.existsByEmail("nobody@test.com")).isFalse();
    }

    @Test
    void existsByUserNo_existing_returnsTrue() {
        assertThat(userRepository.existsByUserNo("S001")).isTrue();
    }

    @Test
    void findByRoleAndStatus_returnsMatchingUsers() {
        Page<User> result = userRepository.findByRoleAndStatus(
                UserRole.STUDENT, UserStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUserNo()).isEqualTo("S001");
    }

    @Test
    void findByRole_returnsAllMatchingRole() {
        Page<User> result = userRepository.findByRole(UserRole.STUDENT, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // ---- helper ----

    private User user(String userNo, String email, UserRole role, UserStatus status) {
        User u = new User();
        u.setUserNo(userNo);
        u.setRealName("测试用户");
        u.setPasswordHash("$2a$10$hash");
        u.setEmail(email);
        u.setRole(role);
        u.setStatus(status);
        return u;
    }
}
