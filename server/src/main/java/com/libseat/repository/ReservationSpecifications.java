package com.libseat.repository;

import com.libseat.entity.Reservation;
import com.libseat.entity.ReservationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class ReservationSpecifications {

    private ReservationSpecifications() {}

    public static Specification<Reservation> forUser(UUID userId) {
        return (root, q, cb) -> userId == null ? cb.conjunction() : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Reservation> withStatus(ReservationStatus status) {
        return (root, q, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Reservation> fromDate(LocalDate from) {
        return (root, q, cb) -> from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("date"), from);
    }

    public static Specification<Reservation> toDate(LocalDate to) {
        return (root, q, cb) -> to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("date"), to);
    }
}
