package com.libseat.repository;

import com.libseat.entity.Reservation;
import com.libseat.entity.ReservationStatus;
import com.libseat.entity.Seat;
import com.libseat.entity.SeatArea;
import com.libseat.entity.SeatStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class SeatSpecifications {

    private SeatSpecifications() {}

    public static Specification<Seat> inLibrary(UUID libraryId) {
        return (root, q, cb) -> cb.equal(root.get("library").get("id"), libraryId);
    }

    public static Specification<Seat> withFloor(Short floor) {
        return (root, q, cb) -> floor == null ? cb.conjunction() : cb.equal(root.get("floor"), floor);
    }

    public static Specification<Seat> withArea(SeatArea area) {
        return (root, q, cb) -> area == null ? cb.conjunction() : cb.equal(root.get("area"), area);
    }

    public static Specification<Seat> withStatus(SeatStatus status) {
        return (root, q, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Seat> hasComputer(Boolean hasComputer) {
        return (root, q, cb) -> hasComputer == null ? cb.conjunction() : cb.equal(root.get("hasComputer"), hasComputer);
    }

    public static Specification<Seat> hasPower(Boolean hasPower) {
        return (root, q, cb) -> hasPower == null ? cb.conjunction() : cb.equal(root.get("hasPower"), hasPower);
    }

    /** 排除在指定日期时间段内已有冲突预约的座位 */
    public static Specification<Seat> availableAt(LocalDate date, LocalTime startTime, LocalTime endTime) {
        return (root, query, cb) -> {
            if (date == null || startTime == null || endTime == null) return cb.conjunction();
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<Reservation> res = sub.from(Reservation.class);
            sub.select(res.get("seat").get("id"))
               .where(cb.and(
                   cb.equal(res.get("date"), date),
                   cb.lessThan(res.get("startTime"), endTime),
                   cb.greaterThan(res.get("endTime"), startTime),
                   res.get("status").in(List.of(
                       ReservationStatus.ACTIVE,
                       ReservationStatus.CHECKED_IN,
                       ReservationStatus.IN_USE
                   ))
               ));
            return cb.not(root.get("id").in(sub));
        };
    }
}
