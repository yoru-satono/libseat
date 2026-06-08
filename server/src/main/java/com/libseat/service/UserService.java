package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.PageResult;
import com.libseat.dto.user.*;
import com.libseat.entity.*;
import com.libseat.exception.BusinessException;
import com.libseat.repository.EmailTokenRepository;
import com.libseat.repository.UserChangeRequestRepository;
import com.libseat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserChangeRequestRepository changeRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailTokenRepository emailTokenRepository;
    private final EmailService emailService;

    @Value("${app.email.token-expiry-hours:24}")
    private int tokenExpiryHours;

    public UserProfileResponse getProfile(UUID userId) {
        return toProfile(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = findUser(userId);

        if (req.getEmail() != null && !req.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail())) {
                throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
            }
            user.setPendingEmail(req.getEmail());

            String token = generateSecureToken();
            EmailToken et = new EmailToken();
            et.setUser(user);
            et.setToken(token);
            et.setType(TokenType.EMAIL_CHANGE);
            et.setExpiresAt(OffsetDateTime.now().plusHours(tokenExpiryHours));
            emailTokenRepository.save(et);
            emailService.sendEmailChange(req.getEmail(), user.getRealName(), token);
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
        }
        if (req.getNewPassword() != null) {
            if (req.getOldPassword() == null
                    || !passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.WRONG_CREDENTIALS);
            }
            user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        }

        return toProfile(user);
    }

    @Transactional
    public ChangeRequestResponse createChangeRequest(UUID userId, ChangeFieldRequest req) {
        User user = findUser(userId);

        String fieldName = req.getFieldName();
        if (!fieldName.equals("userNo") && !fieldName.equals("realName") && !fieldName.equals("department")) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT_ERROR, "不支持修改该字段：" + fieldName);
        }
        if (changeRequestRepository.existsByUserIdAndFieldNameAndStatus(
                userId, fieldName, ChangeRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "该字段已有待审核申请");
        }

        String oldValue = switch (fieldName) {
            case "userNo" -> user.getUserNo();
            case "realName" -> user.getRealName();
            case "department" -> user.getDepartment();
            default -> null;
        };

        UserChangeRequest cr = new UserChangeRequest();
        cr.setUser(user);
        cr.setFieldName(fieldName);
        cr.setOldValue(oldValue);
        cr.setNewValue(req.getNewValue());
        cr.setStatus(ChangeRequestStatus.PENDING);
        changeRequestRepository.save(cr);

        return toChangeRequest(cr);
    }

    public PageResult<ChangeRequestResponse> listMyChangeRequests(UUID userId, int page, int pageSize) {
        Page<UserChangeRequest> pg = changeRequestRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page - 1, pageSize));
        return PageResult.of(
                pg.getContent().stream().map(UserService::toChangeRequest).toList(),
                pg.getTotalElements(), page, pageSize);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    static UserProfileResponse toProfile(User u) {
        return new UserProfileResponse(
                u.getId(), u.getUserNo(), u.getRealName(), u.getEmail(),
                u.getPhone(), u.getDepartment(), u.getRole(), u.getStatus(),
                u.getNoShowCount(), u.getLastLoginAt(), u.getCreatedAt());
    }

    static ChangeRequestResponse toChangeRequest(UserChangeRequest cr) {
        return new ChangeRequestResponse(
                cr.getId(), cr.getFieldName(), cr.getOldValue(), cr.getNewValue(),
                cr.getStatus(), cr.getHandleNote(), cr.getCreatedAt(), cr.getHandledAt());
    }
}
