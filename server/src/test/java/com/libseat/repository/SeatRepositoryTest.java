package com.libseat.repository;

import com.libseat.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import static com.libseat.repository.SeatSpecifications.*;
import static org.assertj.core.api.Assertions.assertThat;

class SeatRepositoryTest extends RepositoryTestBase {

    @Autowired SeatRepository    seatRepository;
    @Autowired LibraryRepository libraryRepository;

    private Library library;

    @BeforeEach
    void setUp() {
        library = libraryRepository.save(library("中心图书馆"));

        seatRepository.save(seat(library, "2F-Q-001", (short) 2, SeatArea.QUIET,  qr(1)));
        seatRepository.save(seat(library, "2F-Q-002", (short) 2, SeatArea.QUIET,  qr(2)));
        seatRepository.save(seat(library, "3F-C-001", (short) 3, SeatArea.COMPUTER, qr(3)));
    }

    @Test
    void findByQrToken_existingToken_returnsSeat() {
        assertThat(seatRepository.findByQrToken(qr(1)))
                .isPresent()
                .hasValueSatisfying(s -> assertThat(s.getSeatNo()).isEqualTo("2F-Q-001"));
    }

    @Test
    void findByQrToken_unknownToken_returnsEmpty() {
        assertThat(seatRepository.findByQrToken("no_such_token_0000000000000000000000000000000000000000000000000")).isEmpty();
    }

    @Test
    void findByFilters_noFilters_returnsAllSeatsInLibrary() {
        Page<Seat> result = seatRepository.findAll(spec(null, null, null), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void findByFilters_withFloor_returnsOnlyThatFloor() {
        Page<Seat> result = seatRepository.findAll(spec((short) 2, null, null), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(s -> s.getFloor() == 2);
    }

    @Test
    void findByFilters_withArea_returnsOnlyThatArea() {
        Page<Seat> result = seatRepository.findAll(spec(null, SeatArea.COMPUTER, null), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getSeatNo()).isEqualTo("3F-C-001");
    }

    @Test
    void findByFilters_withFloorAndArea_returnsMatchingSeats() {
        Page<Seat> result = seatRepository.findAll(spec((short) 2, SeatArea.QUIET, null), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // ---- helpers ----

    private Specification<Seat> spec(Short floor, SeatArea area, SeatStatus status) {
        return Specification.where(inLibrary(library.getId()))
                .and(withFloor(floor))
                .and(withArea(area))
                .and(withStatus(status));
    }

    private String qr(int n) {
        return String.format("%064d", n);
    }

    private Library library(String name) {
        Library l = new Library();
        l.setName(name);
        l.setAddress("测试地址");
        return l;
    }

    private Seat seat(Library lib, String seatNo, short floor, SeatArea area, String qrToken) {
        Seat s = new Seat();
        s.setLibrary(lib);
        s.setSeatNo(seatNo);
        s.setFloor(floor);
        s.setArea(area);
        s.setQrToken(qrToken);
        return s;
    }
}
