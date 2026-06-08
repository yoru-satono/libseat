package com.libseat.repository;

import com.libseat.entity.EmailToken;
import com.libseat.entity.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EmailTokenRepository extends JpaRepository<EmailToken, UUID> {

    Optional<EmailToken> findByToken(String token);

    /** 查找用户最新一条未使用的指定类型令牌 */
    Optional<EmailToken> findFirstByUserIdAndTypeAndUsedAtIsNullOrderByCreatedAtDesc(
            UUID userId, TokenType type);

    /** 清理过期且未使用的令牌（定时任务调用） */
    @Transactional
    @Modifying
    @Query("DELETE FROM EmailToken t WHERE t.expiresAt < :now AND t.usedAt IS NULL")
    int deleteExpired(@Param("now") OffsetDateTime now);
}
