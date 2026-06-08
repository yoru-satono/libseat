package com.libseat.repository;

import com.libseat.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(UUID userId, Boolean isRead, Pageable pageable);

    long countByUserIdAndIsRead(UUID userId, Boolean isRead);

    /** 一键全部已读 */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Notification n
            SET n.isRead = true, n.readAt = :now
            WHERE n.user.id = :userId AND n.isRead = false
            """)
    int markAllRead(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
}
