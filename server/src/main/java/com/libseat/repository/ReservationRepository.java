package com.libseat.repository;

import com.libseat.entity.Reservation;
import com.libseat.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID>, JpaSpecificationExecutor<Reservation> {

    Page<Reservation> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Reservation> findByUserIdAndStatusOrderByCreatedAtDesc(
            UUID userId, ReservationStatus status, Pageable pageable);

    List<Reservation> findByUserIdAndDate(UUID userId, LocalDate date);

    /** 同一座位在指定日期时间段内是否存在冲突预约（用于可用性校验） */
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.seat.id    = :seatId
              AND r.date       = :date
              AND r.startTime  < :endTime
              AND r.endTime    > :startTime
              AND r.status NOT IN :excludedStatuses
            """)
    List<Reservation> findConflicting(
            @Param("seatId")           UUID seatId,
            @Param("date")             LocalDate date,
            @Param("startTime")        LocalTime startTime,
            @Param("endTime")          LocalTime endTime,
            @Param("excludedStatuses") Collection<ReservationStatus> excludedStatuses);

    /** 管理员/定时任务：按状态和日期批量查询 */
    List<Reservation> findByStatusAndDate(ReservationStatus status, LocalDate date);

    Page<Reservation> findByStatusAndDateOrderByCreatedAtDesc(
            ReservationStatus status, LocalDate date, Pageable pageable);

    /** 定时任务：查询指定状态、日期 ≤ 给定日期的预约，JOIN FETCH user 避免 N+1 */
    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.user
            WHERE r.status = :status
              AND r.date <= :date
            """)
    List<Reservation> findByStatusAndDateLessThanEqual(
            @Param("status") ReservationStatus status,
            @Param("date") LocalDate date);

    /** 定时任务：多状态版本，用于批量标记已完成 */
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.status IN :statuses
              AND r.date <= :date
            """)
    List<Reservation> findByStatusInAndDateLessThanEqual(
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("date") LocalDate date);
}
