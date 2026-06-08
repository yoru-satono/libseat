package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.admin.*;
import com.libseat.dto.user.ChangeRequestResponse;
import com.libseat.entity.*;
import com.libseat.entity.AuditLog;
import com.libseat.exception.BusinessException;
import com.libseat.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock UserChangeRequestRepository changeRequestRepository;
    @Mock SystemRulesRepository systemRulesRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AdminService adminService;

    private User user;
    private User admin;
    private UUID userId, adminId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        user = buildUser(userId, UserRole.STUDENT);
        admin = buildUser(adminId, UserRole.ADMIN);
    }

    @Test
    void listUsers_noFilter_returnsPage() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));

        var result = adminService.listUsers(null, null, 1, 20);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().userNo()).isEqualTo("S001");
    }

    @Test
    void getUser_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        AdminUserResponse resp = adminService.getUser(userId);
        assertThat(resp.id()).isEqualTo(userId);
    }

    @Test
    void updateUser_unlockAndResetFailedCount() {
        user.setLockedUntil(java.time.OffsetDateTime.now().plusHours(1));
        user.setFailedLoginCount((short) 5);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        UpdateUserRequest req = new UpdateUserRequest(); req.setStatus(UserStatus.ACTIVE);
        adminService.updateUser(adminId, userId, req);

        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getFailedLoginCount()).isEqualTo((short) 0);
        verify(auditLogRepository).save(argThat(l -> "UPDATE_USER".equals(l.getActionType())));
    }

    @Test
    void handleChangeRequest_approve_appliesChange() {
        UserChangeRequest cr = new UserChangeRequest();
        cr.setId(UUID.randomUUID()); cr.setUser(user);
        cr.setFieldName("realName"); cr.setOldValue("张三"); cr.setNewValue("李四");
        cr.setStatus(ChangeRequestStatus.PENDING);

        when(changeRequestRepository.findById(cr.getId())).thenReturn(Optional.of(cr));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        HandleChangeRequestRequest req = new HandleChangeRequestRequest();
        req.setAction(ChangeRequestStatus.APPROVED);

        ChangeRequestResponse resp = adminService.handleChangeRequest(cr.getId(), adminId, req);

        assertThat(resp.status()).isEqualTo(ChangeRequestStatus.APPROVED);
        assertThat(user.getRealName()).isEqualTo("李四");
        verify(auditLogRepository).save(argThat(l -> "HANDLE_CHANGE_REQUEST".equals(l.getActionType())));
    }

    @Test
    void handleChangeRequest_alreadyHandled_throws() {
        UserChangeRequest cr = new UserChangeRequest();
        cr.setId(UUID.randomUUID()); cr.setUser(user);
        cr.setFieldName("realName"); cr.setStatus(ChangeRequestStatus.APPROVED);

        when(changeRequestRepository.findById(cr.getId())).thenReturn(Optional.of(cr));

        HandleChangeRequestRequest req = new HandleChangeRequestRequest();
        req.setAction(ChangeRequestStatus.REJECTED);
        assertThatThrownBy(() -> adminService.handleChangeRequest(cr.getId(), adminId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARAM_INVALID);
    }

    @Test
    void updateSystemRules_updatesFieldsAndWritesAuditLog() {
        SystemRules existing = new SystemRules();
        existing.setOpenTimeStart(LocalTime.of(7, 0));
        existing.setOpenTimeEnd(LocalTime.of(22, 0));
        existing.setAdvanceDaysMax((short) 7);
        existing.setSingleMinMinutes((short) 30);
        existing.setSingleMaxHours((short) 4);
        existing.setDailyMaxHours((short) 8);
        existing.setCheckinEarlyMinutes((short) 10);
        existing.setCheckinLateMinutes((short) 15);
        existing.setNoShowThreshold((short) 3);
        existing.setSuspendDays((short) 7);
        when(systemRulesRepository.findByLibraryIsNull()).thenReturn(Optional.of(existing));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        UpdateSystemRulesRequest req = new UpdateSystemRulesRequest();
        req.setOpenTimeStart(LocalTime.of(8, 0));
        req.setOpenTimeEnd(LocalTime.of(21, 0));
        req.setAdvanceDaysMax((short) 3);
        req.setSingleMinMinutes((short) 60);
        req.setSingleMaxHours((short) 3);
        req.setDailyMaxHours((short) 6);
        req.setCheckinEarlyMinutes((short) 5);
        req.setCheckinLateMinutes((short) 10);
        req.setNoShowThreshold((short) 2);
        req.setSuspendDays((short) 14);

        SystemRulesResponse resp = adminService.updateSystemRules(adminId, req);

        assertThat(resp.openTimeStart()).isEqualTo(LocalTime.of(8, 0));
        assertThat(resp.advanceDaysMax()).isEqualTo((short) 3);
        verify(auditLogRepository).save(argThat(l -> "UPDATE_SYSTEM_RULES".equals(l.getActionType())));
    }

    @Test
    void getSystemRules_success() {
        SystemRules rules = new SystemRules();
        rules.setOpenTimeStart(LocalTime.of(7, 0));
        rules.setOpenTimeEnd(LocalTime.of(22, 0));
        rules.setAdvanceDaysMax((short) 7);
        rules.setSingleMinMinutes((short) 30);
        rules.setSingleMaxHours((short) 4);
        rules.setDailyMaxHours((short) 8);
        rules.setCheckinEarlyMinutes((short) 10);
        rules.setCheckinLateMinutes((short) 15);
        rules.setNoShowThreshold((short) 3);
        rules.setSuspendDays((short) 7);
        when(systemRulesRepository.findByLibraryIsNull()).thenReturn(Optional.of(rules));

        SystemRulesResponse resp = adminService.getSystemRules();
        assertThat(resp.openTimeStart()).isEqualTo(LocalTime.of(7, 0));
    }

    // ── listAuditLogs ─────────────────────────────────────────────────────

    @Test
    void listAuditLogs_noFilter_returnsPage() {
        AuditLog log = buildAuditLog();
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        var result = adminService.listAuditLogs(null, null, null, null, null, 1, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().actionType()).isEqualTo("UPDATE_USER");
        assertThat(result.items().getFirst().adminName()).isEqualTo("张三");
    }

    @Test
    void listAuditLogs_withFilters_delegatesToRepository() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = adminService.listAuditLogs(
                adminId, "user", userId.toString(),
                OffsetDateTime.now().minusDays(7), OffsetDateTime.now(),
                1, 20);

        assertThat(result.total()).isZero();
        verify(auditLogRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    private AuditLog buildAuditLog() {
        AuditLog log = new AuditLog();
        log.setAdmin(admin);
        log.setActionType("UPDATE_USER");
        log.setTargetType("user");
        log.setTargetId(userId.toString());
        log.setDetail(Map.of("status", "ACTIVE"));
        return log;
    }

    private User buildUser(UUID id, UserRole role) {
        User u = new User();
        u.setId(id); u.setUserNo("S001"); u.setRealName("张三");
        u.setEmail("a@b.com"); u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        u.setFailedLoginCount((short) 0); u.setNoShowCount((short) 0);
        return u;
    }
}
