package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.user.*;
import com.libseat.entity.*;
import com.libseat.exception.BusinessException;
import com.libseat.repository.UserChangeRequestRepository;
import com.libseat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserChangeRequestRepository changeRequestRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock com.libseat.repository.EmailTokenRepository emailTokenRepository;
    @Mock EmailService emailService;
    @InjectMocks UserService userService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId); user.setUserNo("S001"); user.setRealName("张三");
        user.setEmail("a@b.com"); user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount((short) 0); user.setNoShowCount((short) 0);
    }

    @Test
    void getProfile_returnsDto() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        UserProfileResponse resp = userService.getProfile(userId);
        assertThat(resp.userNo()).isEqualTo("S001");
        assertThat(resp.email()).isEqualTo("a@b.com");
    }

    @Test
    void getProfile_notFound_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getProfile(userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void updateProfile_changeEmail_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@b.com")).thenReturn(false);
        when(emailTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest(); req.setEmail("new@b.com");
        userService.updateProfile(userId, req);

        // Email change goes through verification flow: email stays unchanged, pendingEmail is set
        assertThat(user.getEmail()).isEqualTo("a@b.com");
        assertThat(user.getPendingEmail()).isEqualTo("new@b.com");
        verify(emailService).sendEmailChange(eq("new@b.com"), any(), any());
    }

    @Test
    void updateProfile_duplicateEmail_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@b.com")).thenReturn(true);

        UpdateProfileRequest req = new UpdateProfileRequest(); req.setEmail("new@b.com");
        assertThatThrownBy(() -> userService.updateProfile(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_DUPLICATE);
    }

    @Test
    void updateProfile_changePassword_wrongOldPassword_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setOldPassword("wrong"); req.setNewPassword("newPass123");
        assertThatThrownBy(() -> userService.updateProfile(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WRONG_CREDENTIALS);
    }

    @Test
    void createChangeRequest_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(changeRequestRepository.existsByUserIdAndFieldNameAndStatus(userId, "realName", ChangeRequestStatus.PENDING))
                .thenReturn(false);
        when(changeRequestRepository.save(any())).thenAnswer(i -> {
            UserChangeRequest cr = i.getArgument(0);
            cr.setId(UUID.randomUUID());
            return cr;
        });

        ChangeFieldRequest req = new ChangeFieldRequest(); req.setFieldName("realName"); req.setNewValue("李四");
        ChangeRequestResponse resp = userService.createChangeRequest(userId, req);
        assertThat(resp.fieldName()).isEqualTo("realName");
        assertThat(resp.newValue()).isEqualTo("李四");
    }

    @Test
    void createChangeRequest_pendingExists_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(changeRequestRepository.existsByUserIdAndFieldNameAndStatus(userId, "realName", ChangeRequestStatus.PENDING))
                .thenReturn(true);

        ChangeFieldRequest req = new ChangeFieldRequest(); req.setFieldName("realName"); req.setNewValue("李四");
        assertThatThrownBy(() -> userService.createChangeRequest(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARAM_INVALID);
    }

    @Test
    void listMyChangeRequests_returnsPage() {
        when(changeRequestRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = userService.listMyChangeRequests(userId, 1, 20);
        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isEqualTo(0);
    }
}
