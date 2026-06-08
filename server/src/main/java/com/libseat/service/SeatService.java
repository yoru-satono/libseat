package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.PageResult;
import com.libseat.dto.seat.*;
import com.libseat.entity.*;
import com.libseat.exception.BusinessException;
import com.libseat.repository.LibraryRepository;
import com.libseat.repository.SeatRepository;
import com.libseat.repository.SeatSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final LibraryRepository libraryRepository;

    public PageResult<SeatResponse> listSeats(Short floor, SeatArea area,
                                              Boolean hasComputer, Boolean hasPower,
                                              LocalDate date, LocalTime startTime, LocalTime endTime,
                                              int page, int pageSize) {
        Specification<Seat> spec = Specification
                .where(SeatSpecifications.withStatus(SeatStatus.AVAILABLE))
                .and(SeatSpecifications.withFloor(floor))
                .and(SeatSpecifications.withArea(area))
                .and(SeatSpecifications.hasComputer(hasComputer))
                .and(SeatSpecifications.hasPower(hasPower))
                .and(SeatSpecifications.availableAt(date, startTime, endTime));

        Page<Seat> pg = seatRepository.findAll(spec, PageRequest.of(page - 1, pageSize, Sort.by("floor", "seatNo")));
        return PageResult.of(
                pg.getContent().stream().map(SeatService::toResponse).toList(),
                pg.getTotalElements(), page, pageSize);
    }

    public SeatResponse getSeat(UUID seatId) {
        return toResponse(findSeat(seatId));
    }

    @Transactional
    public SeatResponse createSeat(CreateSeatRequest req) {
        Library library = libraryRepository.findById(req.getLibraryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "图书馆不存在"));

        Seat seat = new Seat();
        seat.setLibrary(library);
        seat.setSeatNo(req.getSeatNo());
        seat.setFloor(req.getFloor());
        seat.setArea(req.getArea());
        seat.setHasComputer(Boolean.TRUE.equals(req.getHasComputer()));
        seat.setHasPower(Boolean.TRUE.equals(req.getHasPower()));
        seat.setHasWindow(Boolean.TRUE.equals(req.getHasWindow()));
        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setPosX(req.getPosX());
        seat.setPosY(req.getPosY());
        seatRepository.save(seat);

        return toResponse(seat);
    }

    @Transactional
    public SeatResponse updateSeat(UUID seatId, UpdateSeatRequest req) {
        Seat seat = findSeat(seatId);

        if (req.getSeatNo() != null) seat.setSeatNo(req.getSeatNo());
        if (req.getFloor() != null) seat.setFloor(req.getFloor());
        if (req.getArea() != null) seat.setArea(req.getArea());
        if (req.getHasComputer() != null) seat.setHasComputer(req.getHasComputer());
        if (req.getHasPower() != null) seat.setHasPower(req.getHasPower());
        if (req.getHasWindow() != null) seat.setHasWindow(req.getHasWindow());
        if (req.getStatus() != null) seat.setStatus(req.getStatus());
        if (req.getPosX() != null) seat.setPosX(req.getPosX());
        if (req.getPosY() != null) seat.setPosY(req.getPosY());

        return toResponse(seat);
    }

    @Transactional
    public void deleteSeat(UUID seatId) {
        Seat seat = findSeat(seatId);
        seatRepository.delete(seat);
    }

    private Seat findSeat(UUID seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "座位不存在"));
    }

    static SeatResponse toResponse(Seat s) {
        return new SeatResponse(
                s.getId(),
                s.getLibrary().getId(),
                s.getLibrary().getName(),
                s.getSeatNo(),
                s.getFloor(),
                s.getArea(),
                s.getHasComputer(),
                s.getHasPower(),
                s.getHasWindow(),
                s.getStatus(),
                s.getPosX(),
                s.getPosY());
    }
}
