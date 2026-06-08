package com.libseat.repository;

import com.libseat.entity.Waitlist;
import com.libseat.entity.WaitlistStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WaitlistRepository extends JpaRepository<Waitlist, UUID> {

    Page<Waitlist> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Waitlist> findByUserIdAndStatusOrderByCreatedAtDesc(
            UUID userId, WaitlistStatus status, Pageable pageable);

    /** 某座位指定时段按创建时间排序的等待队列（座位变为可用时按序通知） */
    @Query("""
            SELECT w FROM Waitlist w
            WHERE w.seat.id   = :seatId
              AND w.date       = :date
              AND w.startTime  = :startTime
              AND w.endTime    = :endTime
              AND w.status     = 'WAITING'
            ORDER BY w.createdAt ASC
            """)
    List<Waitlist> findWaitingQueue(
            @Param("seatId")    UUID seatId,
            @Param("date")      LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime")   LocalTime endTime);

    /** 用户是否已在该时段的等待队列中 */
    boolean existsByUserIdAndSeatIdAndDateAndStartTimeAndEndTimeAndStatusIn(
            UUID userId, UUID seatId, LocalDate date,
            LocalTime startTime, LocalTime endTime,
            Collection<WaitlistStatus> statuses);
}
