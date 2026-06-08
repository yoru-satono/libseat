package com.libseat.repository;

import com.libseat.entity.ChangeRequestStatus;
import com.libseat.entity.UserChangeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserChangeRequestRepository extends JpaRepository<UserChangeRequest, UUID> {

    Page<UserChangeRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** 管理员审核队列 */
    Page<UserChangeRequest> findByStatusOrderByCreatedAtDesc(
            ChangeRequestStatus status, Pageable pageable);

    /** 同字段是否已有待审核申请（防止重复提交） */
    boolean existsByUserIdAndFieldNameAndStatus(
            UUID userId, String fieldName, ChangeRequestStatus status);
}
