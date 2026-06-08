package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.auth.*;
import com.libseat.entity.*;
import com.libseat.exception.BusinessException;
import com.libseat.repository.EmailTokenRepository;
import com.libseat.repository.NotificationRepository;
import com.libseat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock EmailTokenRepository emailTokenRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock EmailService emailService;
    @InjectMocks AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "tokenExpiryHours", 24);
    }

    // ── register ──────────────────────────────────────────────────────────

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setUserNo("S001"); req.setRealName("张三");
        req.setPassword("pass"); req.setEmail("a@b.com");

        when(userRepository.existsByUserNo("S001")).thenReturn(false);
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("hashed");

        authService.register(req);

        verify(userRepository).save(any(User.class));
        verify(emailService).sendActivation(eq("a@b.com"), eq("张三"), anyString());
    }

    @Test
    void register_duplicateUserNo_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUserNo("S001"); req.setEmail("a@b.com");
        when(userRepository.existsByUserNo("S001")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NO_DUPLICATE);
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUserNo("S001"); req.setEmail("a@b.com");
        when(userRepository.existsByUserNo("S001")).thenReturn(false);
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_DUPLICATE);
    }

    // ── login ─────────────────────────────────────────────────────────────

    @Test
    void login_success_returnsTokenPair() {
        User user = activeUser();
        when(userRepository.findByUserNo("S001")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user.getId())).thenReturn("refresh");
        when(jwtService.getAccessExpiry()).thenReturn(7200L);

        LoginRequest req = new LoginRequest(); req.setUserNo("S001"); req.setPassword("pass");
        TokenPairResponse resp = authService.login(req);

        assertThat(resp.accessToken()).isEqualTo("access");
        assertThat(user.getFailedLoginCount()).isEqualTo((short) 0);
    }

    @Test
    void login_wrongPassword_incrementsFailedCount() {
        User user = activeUser();
        when(userRepository.findByUserNo("S001")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        LoginRequest req = new LoginRequest(); req.setUserNo("S001"); req.setPassword("wrong");
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WRONG_CREDENTIALS);

        assertThat(user.getFailedLoginCount()).isEqualTo((short) 1);
    }

    @Test
    void login_fiveFailures_locksAccount() {
        User user = activeUser();
        user.setFailedLoginCount((short) 4);
        when(userRepository.findByUserNo("S001")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        LoginRequest req = new LoginRequest(); req.setUserNo("S001"); req.setPassword("wrong");
        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(BusinessException.class);

        assertThat(user.getLockedUntil()).isNotNull().isAfter(OffsetDateTime.now());
        assertThat(user.getFailedLoginCount()).isEqualTo((short) 0);
    }

    @Test
    void login_lockedAccount_throws() {
        User user = activeUser();
        user.setLockedUntil(OffsetDateTime.now().plusHours(1));
        when(userRepository.findByUserNo("S001")).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest(); req.setUserNo("S001"); req.setPassword("pass");
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_LOCKED);
    }

    @Test
    void login_inactiveAccount_throws() {
        User user = activeUser(); user.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByUserNo("S001")).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest(); req.setUserNo("S001"); req.setPassword("pass");
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_INACTIVE);
    }

    // ── activate ──────────────────────────────────────────────────────────

    @Test
    void activate_success() {
        User user = activeUser(); user.setStatus(UserStatus.INACTIVE);
        EmailToken et = new EmailToken();
        et.setUser(user); et.setToken("tok");
        et.setType(TokenType.ACTIVATION);
        et.setExpiresAt(OffsetDateTime.now().plusHours(1));

        when(emailTokenRepository.findByToken("tok")).thenReturn(Optional.of(et));

        authService.activate("tok");

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(et.getUsedAt()).isNotNull();
    }

    @Test
    void activate_expiredToken_throws() {
        User user = activeUser();
        EmailToken et = new EmailToken();
        et.setUser(user); et.setToken("tok");
        et.setType(TokenType.ACTIVATION);
        et.setExpiresAt(OffsetDateTime.now().minusHours(1));

        when(emailTokenRepository.findByToken("tok")).thenReturn(Optional.of(et));

        assertThatThrownBy(() -> authService.activate("tok"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_EXPIRED);
    }

    // ── refresh ───────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_returnsNewPair() {
        UUID id = UUID.randomUUID();
        User user = activeUser(); user.setId(id);

        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.get("tokenType", String.class)).thenReturn("refresh");
        when(claims.getSubject()).thenReturn(id.toString());
        when(jwtService.parse("ref")).thenReturn(claims);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("newAccess");
        when(jwtService.generateRefreshToken(id)).thenReturn("newRefresh");

        RefreshTokenRequest req = new RefreshTokenRequest(); req.setRefreshToken("ref");
        TokenPairResponse resp = authService.refresh(req);

        assertThat(resp.accessToken()).isEqualTo("newAccess");
    }

    // ── confirmEmailChange ────────────────────────────────────────────────

    @Test
    void confirmEmailChange_success() {
        User user = activeUser();
        user.setPendingEmail("new@b.com");
        EmailToken et = new EmailToken();
        et.setUser(user); et.setToken("tok");
        et.setType(TokenType.EMAIL_CHANGE);
        et.setExpiresAt(OffsetDateTime.now().plusHours(1));

        when(emailTokenRepository.findByToken("tok")).thenReturn(Optional.of(et));

        authService.confirmEmailChange("tok");

        assertThat(user.getEmail()).isEqualTo("new@b.com");
        assertThat(user.getPendingEmail()).isNull();
        assertThat(et.getUsedAt()).isNotNull();
    }

    @Test
    void confirmEmailChange_expiredToken_throws() {
        User user = activeUser();
        user.setPendingEmail("new@b.com");
        EmailToken et = new EmailToken();
        et.setUser(user); et.setToken("tok");
        et.setType(TokenType.EMAIL_CHANGE);
        et.setExpiresAt(OffsetDateTime.now().minusHours(1));

        when(emailTokenRepository.findByToken("tok")).thenReturn(Optional.of(et));

        assertThatThrownBy(() -> authService.confirmEmailChange("tok"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void confirmEmailChange_noPendingEmail_throws() {
        User user = activeUser(); // pendingEmail 为 null
        EmailToken et = new EmailToken();
        et.setUser(user); et.setToken("tok");
        et.setType(TokenType.EMAIL_CHANGE);
        et.setExpiresAt(OffsetDateTime.now().plusHours(1));

        when(emailTokenRepository.findByToken("tok")).thenReturn(Optional.of(et));

        assertThatThrownBy(() -> authService.confirmEmailChange("tok"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_INVALID);
    }

    // ── helper ────────────────────────────────────────────────────────────

    private User activeUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUserNo("S001");
        u.setPasswordHash("hashed");
        u.setEmail("a@b.com");
        u.setRealName("张三");
        u.setRole(UserRole.STUDENT);
        u.setStatus(UserStatus.ACTIVE);
        u.setFailedLoginCount((short) 0);
        u.setNoShowCount((short) 0);
        return u;
    }
}
