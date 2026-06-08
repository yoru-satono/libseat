package com.libseat.controller;

import com.libseat.entity.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

/**
 * Controller 测试基类，提供统一的测试用户和认证对象构造工具。
 * 子类通过 SecurityMockMvcRequestPostProcessors.authentication(auth) 传入认证。
 */
abstract class ControllerTestBase {

    protected static final UUID TEST_USER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    protected static final UUID TEST_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    protected static User buildUser(UUID id, UserRole role) {
        User u = new User();
        u.setId(id); u.setUserNo("S001"); u.setRealName("测试用户");
        u.setEmail("test@test.com"); u.setRole(role); u.setStatus(UserStatus.ACTIVE);
        u.setFailedLoginCount((short) 0); u.setNoShowCount((short) 0);
        return u;
    }

    protected static Authentication userAuth() {
        User u = buildUser(TEST_USER_ID, UserRole.STUDENT);
        return new UsernamePasswordAuthenticationToken(
                u, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    protected static Authentication adminAuth() {
        User u = buildUser(TEST_ADMIN_ID, UserRole.ADMIN);
        return new UsernamePasswordAuthenticationToken(
                u, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
