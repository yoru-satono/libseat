package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.seat.*;
import com.libseat.entity.*;
import com.libseat.exception.BusinessException;
import com.libseat.repository.LibraryRepository;
import com.libseat.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock SeatRepository seatRepository;
    @Mock LibraryRepository libraryRepository;
    @InjectMocks SeatService seatService;

    private UUID seatId;
    private Seat seat;
    private Library library;

    @BeforeEach
    void setUp() {
        seatId = UUID.randomUUID();
        library = new Library();
        library.setId(UUID.randomUUID()); library.setName("总馆");

        seat = new Seat();
        seat.setId(seatId); seat.setLibrary(library);
        seat.setSeatNo("3F-001"); seat.setFloor((short) 3);
        seat.setArea(SeatArea.QUIET); seat.setStatus(SeatStatus.AVAILABLE);
        seat.setHasComputer(false); seat.setHasPower(true); seat.setHasWindow(false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listSeats_noFilter_returnsPage() {
        when(seatRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(seat)));

        var result = seatService.listSeats(null, null, null, null, null, null, null, 1, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().seatNo()).isEqualTo("3F-001");
    }

    @Test
    void getSeat_success() {
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        SeatResponse resp = seatService.getSeat(seatId);
        assertThat(resp.seatNo()).isEqualTo("3F-001");
        assertThat(resp.libraryName()).isEqualTo("总馆");
    }

    @Test
    void getSeat_notFound_throws() {
        when(seatRepository.findById(seatId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> seatService.getSeat(seatId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void createSeat_success() {
        when(libraryRepository.findById(library.getId())).thenReturn(Optional.of(library));
        when(seatRepository.save(any())).thenReturn(seat);

        CreateSeatRequest req = new CreateSeatRequest();
        req.setLibraryId(library.getId()); req.setSeatNo("3F-001");
        req.setFloor((short) 3); req.setArea(SeatArea.QUIET);

        SeatResponse resp = seatService.createSeat(req);
        assertThat(resp).isNotNull();
        verify(seatRepository).save(any(Seat.class));
    }

    @Test
    void createSeat_libraryNotFound_throws() {
        when(libraryRepository.findById(any())).thenReturn(Optional.empty());

        CreateSeatRequest req = new CreateSeatRequest();
        req.setLibraryId(UUID.randomUUID()); req.setSeatNo("3F-001");
        req.setFloor((short) 3); req.setArea(SeatArea.QUIET);

        assertThatThrownBy(() -> seatService.createSeat(req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void deleteSeat_success() {
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        seatService.deleteSeat(seatId);
        verify(seatRepository).delete(seat);
    }
}
