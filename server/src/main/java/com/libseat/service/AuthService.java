package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.auth.*;
import com.libseat.entity.*;
import com.libseat.exception.BusinessException;
import com.libseat.repository.EmailTokenRepository;
import com.libseat.repository.NotificationRepository;
import com.libseat.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 30;

    private final UserRepository userRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Value("${app.email.token-expiry-hours:24}")
    private int tokenExpiryHours;

    @Transactional
    public void register(RegisterRequest req) {
        if (userRepository.existsByUserNo(req.getUserNo())) {
            throw new BusinessException(ErrorCode.USER_NO_DUPLICATE);
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }

        User user = new User();
        user.setUserNo(req.getUserNo());
        user.setRealName(req.getRealName());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setDepartment(req.getDepartment());
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        String token = generateSecureToken();
        EmailToken et = new EmailToken();
        et.setUser(user);
        et.setToken(token);
        et.setType(TokenType.ACTIVATION);
        et.setExpiresAt(OffsetDateTime.now().plusHours(tokenExpiryHours));
        emailTokenRepository.save(et);

        emailService.sendActivation(user.getEmail(), user.getRealName(), token);
    }

    @Transactional
    public void activate(String token) {
        EmailToken et = emailTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
        validateEmailToken(et, TokenType.ACTIVATION);

        et.setUsedAt(OffsetDateTime.now());
        User user = et.getUser();
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(OffsetDateTime.now());
    }

    @Transactional
    public TokenPairResponse login(LoginRequest req) {
        User user = userRepository.findByUserNo(req.getUserNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.WRONG_CREDENTIALS));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            short failed = (short) (user.getFailedLoginCount() + 1);
            user.setFailedLoginCount(failed);
            if (failed >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(OffsetDateTime.now().plusMinutes(LOCK_MINUTES));
                user.setFailedLoginCount((short) 0);
                Notification notif = new Notification();
                notif.setUser(user);
                notif.setType(NotificationType.ACCOUNT_LOCKED);
                notif.setTitle("账号已被锁定");
                notif.setContent(String.format("您的账号因连续 %d 次密码错误已被锁定 %d 分钟，请稍后重试。",
                        MAX_FAILED_ATTEMPTS, LOCK_MINUTES));
                notificationRepository.save(notif);
            }
            throw new BusinessException(ErrorCode.WRONG_CREDENTIALS);
        }

        user.setFailedLoginCount((short) 0);
        user.setLockedUntil(null);
        user.setLastLoginAt(OffsetDateTime.now());

        return buildTokenPair(user);
    }

    public TokenPairResponse refresh(RefreshTokenRequest req) {
        Claims claims;
        try {
            claims = jwtService.parse(req.getRefreshToken());
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        if (!"refresh".equals(claims.get("tokenType", String.class))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));

        return buildTokenPair(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmail(req.getEmail()).ifPresent(user -> {
            String token = generateSecureToken();
            EmailToken et = new EmailToken();
            et.setUser(user);
            et.setToken(token);
            et.setType(TokenType.PASSWORD_RESET);
            et.setExpiresAt(OffsetDateTime.now().plusHours(tokenExpiryHours));
            emailTokenRepository.save(et);
            emailService.sendPasswordReset(user.getEmail(), user.getRealName(), token);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        EmailToken et = emailTokenRepository.findByToken(req.getToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
        validateEmailToken(et, TokenType.PASSWORD_RESET);

        et.setUsedAt(OffsetDateTime.now());
        et.getUser().setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
    }

    @Transactional
    public void confirmEmailChange(String token) {
        EmailToken et = emailTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
        validateEmailToken(et, TokenType.EMAIL_CHANGE);

        User user = et.getUser();
        if (user.getPendingEmail() == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        et.setUsedAt(OffsetDateTime.now());
        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
    }

    private void validateEmailToken(EmailToken et, TokenType expectedType) {
        if (et.getType() != expectedType || et.getUsedAt() != null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        if (et.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }
    }

    private TokenPairResponse buildTokenPair(User user) {
        return new TokenPairResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user.getId()),
                jwtService.getAccessExpiry());
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
