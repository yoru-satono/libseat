package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.PageResult;
import com.libseat.dto.admin.*;
import com.libseat.dto.user.ChangeRequestResponse;
import com.libseat.entity.*;
import com.libseat.exception.BusinessException;
import com.libseat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final UserChangeRequestRepository changeRequestRepository;
    private final SystemRulesRepository systemRulesRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    // ── 用户管理 ───────────────────────────────────────────────────────────

    public PageResult<AdminUserResponse> listUsers(UserRole role, UserStatus status, int page, int pageSize) {
        Page<User> pg;
        PageRequest pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (role != null && status != null) {
            pg = userRepository.findByRoleAndStatus(role, status, pageable);
        } else if (role != null) {
            pg = userRepository.findByRole(role, pageable);
        } else if (status != null) {
            pg = userRepository.findByStatus(status, pageable);
        } else {
            pg = userRepository.findAll(pageable);
        }
        return PageResult.of(
                pg.getContent().stream().map(AdminService::toAdminUser).toList(),
                pg.getTotalElements(), page, pageSize);
    }

    public AdminUserResponse getUser(UUID userId) {
        return toAdminUser(findUser(userId));
    }

    @Transactional
    public AdminUserResponse updateUser(UUID adminId, UUID userId, UpdateUserRequest req) {
        User user = findUser(userId);

        Map<String, Object> before = Map.of(
                "role", user.getRole(), "status", user.getStatus(),
                "noShowCount", user.getNoShowCount());

        if (req.getRole() != null) user.setRole(req.getRole());
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
            if (req.getStatus() == UserStatus.ACTIVE) {
                user.setLockedUntil(null);
                user.setSuspendedUntil(null);
                user.setFailedLoginCount((short) 0);
            }
        }
        if (req.getNewPassword() != null) {
            user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        }
        if (Boolean.TRUE.equals(req.getResetNoShowCount())) {
            user.setNoShowCount((short) 0);
        }

        Map<String, Object> after = Map.of(
                "role", user.getRole(), "status", user.getStatus(),
                "noShowCount", user.getNoShowCount());

        writeAuditLog(findUser(adminId), "UPDATE_USER", "user", userId.toString(),
                Map.of("before", before, "after", after));

        return toAdminUser(user);
    }

    // ── 预约管理 ───────────────────────────────────────────────────────────

    public PageResult<AdminReservationResponse> listAllReservations(
            ReservationStatus status, LocalDate dateFrom, LocalDate dateTo, int page, int pageSize) {
        Specification<Reservation> spec = Specification
                .where(ReservationSpecifications.withStatus(status))
                .and(ReservationSpecifications.fromDate(dateFrom))
                .and(ReservationSpecifications.toDate(dateTo));
        Page<Reservation> pg = reservationRepository.findAll(
                spec, PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(
                pg.getContent().stream().map(AdminService::toAdminReservation).toList(),
                pg.getTotalElements(), page, pageSize);
    }

    public java.util.List<AdminReservationResponse> listAllReservationsForExport(
            UUID userId, ReservationStatus status, LocalDate dateFrom, LocalDate dateTo) {
        Specification<Reservation> spec = Specification
                .where(ReservationSpecifications.withStatus(status))
                .and(ReservationSpecifications.fromDate(dateFrom))
                .and(ReservationSpecifications.toDate(dateTo));
        if (userId != null) {
            spec = spec.and(ReservationSpecifications.forUser(userId));
        }
        java.util.List<Reservation> rows = reservationRepository.findAll(
                spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (rows.size() > 5000) {
            throw new BusinessException(ErrorCode.EXPORT_LIMIT_EXCEEDED);
        }
        return rows.stream().map(AdminService::toAdminReservation).toList();
    }

    public AdminReservationResponse getAdminReservation(UUID reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "预约不存在"));
        return toAdminReservation(r);
    }

    @Transactional
    public void adminCancelReservation(UUID adminId, UUID reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "预约不存在"));
        if (r.getStatus() != ReservationStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_STARTED, "仅可取消状态为 ACTIVE 的预约");
        }
        r.setStatus(ReservationStatus.CANCELLED);
        r.setCancelledAt(OffsetDateTime.now());
        r.setCancelReason("管理员取消");

        writeAuditLog(findUser(adminId), "CANCEL_RESERVATION", "reservation",
                reservationId.toString(), Map.of("cancelledBy", "admin"));
    }

    // ── 信息修改申请 ───────────────────────────────────────────────────────

    public PageResult<AdminChangeRequestResponse> listPendingChangeRequests(int page, int pageSize) {
        Page<UserChangeRequest> pg = changeRequestRepository.findByStatusOrderByCreatedAtDesc(
                ChangeRequestStatus.PENDING,
                PageRequest.of(page - 1, pageSize));
        return PageResult.of(
                pg.getContent().stream().map(AdminService::toAdminChangeRequest).toList(),
                pg.getTotalElements(), page, pageSize);
    }

    @Transactional
    public ChangeRequestResponse handleChangeRequest(UUID requestId, UUID adminId,
                                                     HandleChangeRequestRequest req) {
        UserChangeRequest cr = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "申请不存在"));
        if (cr.getStatus() != ChangeRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "该申请已处理");
        }

        User admin = findUser(adminId);
        cr.setStatus(req.getAction());
        cr.setHandledBy(admin);
        cr.setHandleNote(req.getHandleNote());
        cr.setHandledAt(OffsetDateTime.now());

        if (req.getAction() == ChangeRequestStatus.APPROVED) {
            applyFieldChange(cr.getUser(), cr.getFieldName(), cr.getNewValue());
        }

        writeAuditLog(admin, "HANDLE_CHANGE_REQUEST", "change_request", requestId.toString(),
                Map.of("field", cr.getFieldName(), "action", req.getAction(),
                        "oldValue", cr.getOldValue() != null ? cr.getOldValue() : "",
                        "newValue", cr.getNewValue()));

        return UserService.toChangeRequest(cr);
    }

    private void applyFieldChange(User user, String fieldName, String newValue) {
        switch (fieldName) {
            case "userNo" -> {
                if (userRepository.existsByUserNo(newValue)) {
                    throw new BusinessException(ErrorCode.USER_NO_DUPLICATE);
                }
                user.setUserNo(newValue);
            }
            case "realName" -> user.setRealName(newValue);
            case "department" -> user.setDepartment(newValue);
            default -> throw new BusinessException(ErrorCode.PARAM_FORMAT_ERROR, "未知字段：" + fieldName);
        }
    }

    // ── 系统规则 ───────────────────────────────────────────────────────────

    public SystemRulesResponse getSystemRules() {
        SystemRules rules = systemRulesRepository.findByLibraryIsNull()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "全局规则未配置"));
        return toRulesResponse(rules);
    }

    @Transactional
    public SystemRulesResponse updateSystemRules(UUID adminId, UpdateSystemRulesRequest req) {
        SystemRules rules = systemRulesRepository.findByLibraryIsNull()
                .orElseGet(SystemRules::new);

        rules.setOpenTimeStart(req.getOpenTimeStart());
        rules.setOpenTimeEnd(req.getOpenTimeEnd());
        rules.setAdvanceDaysMax(req.getAdvanceDaysMax());
        rules.setSingleMinMinutes(req.getSingleMinMinutes());
        rules.setSingleMaxHours(req.getSingleMaxHours());
        rules.setDailyMaxHours(req.getDailyMaxHours());
        rules.setCheckinEarlyMinutes(req.getCheckinEarlyMinutes());
        rules.setCheckinLateMinutes(req.getCheckinLateMinutes());
        rules.setNoShowThreshold(req.getNoShowThreshold());
        rules.setSuspendDays(req.getSuspendDays());
        User admin = findUser(adminId);
        rules.setUpdatedBy(admin);
        systemRulesRepository.save(rules);

        writeAuditLog(admin, "UPDATE_SYSTEM_RULES", "rule", String.valueOf(rules.getId()),
                Map.of("openTimeStart", req.getOpenTimeStart().toString(),
                        "openTimeEnd", req.getOpenTimeEnd().toString(),
                        "advanceDaysMax", req.getAdvanceDaysMax(),
                        "singleMaxHours", req.getSingleMaxHours(),
                        "dailyMaxHours", req.getDailyMaxHours()));

        return toRulesResponse(rules);
    }

    // ── 审计日志查询 ───────────────────────────────────────────────────────

    public PageResult<AuditLogResponse> listAuditLogs(
            UUID adminId, String targetType, String targetId,
            OffsetDateTime dateFrom, OffsetDateTime dateTo,
            int page, int pageSize) {
        Specification<AuditLog> spec = (r, q, cb) -> cb.conjunction();
        if (adminId != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("admin").get("id"), adminId));
        }
        if (targetType != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("targetType"), targetType));
        }
        if (targetId != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("targetId"), targetId));
        }
        if (dateFrom != null) {
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("createdAt"), dateFrom));
        }
        if (dateTo != null) {
            spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("createdAt"), dateTo));
        }
        Page<AuditLog> pg = auditLogRepository.findAll(
                spec, PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(
                pg.getContent().stream().map(AdminService::toAuditLogResponse).toList(),
                pg.getTotalElements(), page, pageSize);
    }

    // ── 私有工具 ───────────────────────────────────────────────────────────

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void writeAuditLog(User admin, String actionType, String targetType,
                               String targetId, Map<String, Object> detail) {
        AuditLog log = new AuditLog();
        log.setAdmin(admin);
        log.setActionType(actionType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        auditLogRepository.save(log);
    }

    static AdminUserResponse toAdminUser(User u) {
        return new AdminUserResponse(
                u.getId(), u.getUserNo(), u.getRealName(), u.getEmail(),
                u.getPhone(), u.getDepartment(), u.getRole(), u.getStatus(),
                u.getFailedLoginCount(), u.getNoShowCount(),
                u.getLockedUntil(), u.getSuspendedUntil(),
                u.getLastLoginAt(), u.getCreatedAt());
    }

    static AdminReservationResponse toAdminReservation(Reservation r) {
        Seat seat = r.getSeat();
        User user = r.getUser();
        return new AdminReservationResponse(
                r.getId(),
                user.getId(), user.getUserNo(), user.getRealName(),
                seat.getId(), seat.getSeatNo(), seat.getLibrary().getName(),
                seat.getFloor(), r.getDate(), r.getStartTime(), r.getEndTime(),
                seat.getArea(), r.getStatus(),
                r.getCheckinAt(), r.getCancelledAt(), r.getCreatedAt());
    }

    static AdminChangeRequestResponse toAdminChangeRequest(UserChangeRequest cr) {
        User user = cr.getUser();
        return new AdminChangeRequestResponse(
                cr.getId(),
                user.getId(), user.getUserNo(), user.getRealName(),
                cr.getFieldName(), cr.getOldValue(), cr.getNewValue(),
                cr.getStatus(), cr.getHandleNote(),
                cr.getCreatedAt(), cr.getHandledAt());
    }

    static AuditLogResponse toAuditLogResponse(AuditLog l) {
        User admin = l.getAdmin();
        return new AuditLogResponse(
                l.getId(), admin.getId(), admin.getRealName(),
                l.getActionType(), l.getTargetType(), l.getTargetId(),
                l.getDetail(), l.getIpAddress(), l.getCreatedAt());
    }

    static SystemRulesResponse toRulesResponse(SystemRules r) {
        UUID libraryId = r.getLibrary() != null ? r.getLibrary().getId() : null;
        String libraryName = r.getLibrary() != null ? r.getLibrary().getName() : null;
        return new SystemRulesResponse(
                r.getId(), libraryId, libraryName,
                r.getOpenTimeStart(), r.getOpenTimeEnd(),
                r.getAdvanceDaysMax(), r.getSingleMinMinutes(), r.getSingleMaxHours(),
                r.getDailyMaxHours(), r.getCheckinEarlyMinutes(), r.getCheckinLateMinutes(),
                r.getNoShowThreshold(), r.getSuspendDays(), r.getUpdatedAt());
    }
}
