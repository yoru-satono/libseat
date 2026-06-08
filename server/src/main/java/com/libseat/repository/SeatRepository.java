package com.libseat.repository;

import com.libseat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID>, JpaSpecificationExecutor<Seat> {

    Optional<Seat> findByQrToken(String qrToken);

    boolean existsByLibraryId(UUID libraryId);
}
