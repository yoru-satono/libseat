package com.libseat.repository;

import com.libseat.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationRepositoryTest extends RepositoryTestBase {

    @Autowired ReservationRepository reservationRepository;
    @Autowired UserRepository        userRepository;
    @Autowired SeatRepository        seatRepository;
    @Autowired LibraryRepository     libraryRepository;

    private static final List<ReservationStatus> EXCLUDED =
            List.of(ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW, ReservationStatus.COMPLETED);

    private User    user;
    private Seat    seat;
    private LocalDate today = LocalDate.of(2026, 5, 8);

    @BeforeEach
    void setUp() {
        Library lib = libraryRepository.save(library());
        seat = seatRepository.save(seat(lib));
        user = userRepository.save(user());

        // 已有预约：09:00 – 11:00，ACTIVE
        reservationRepository.save(reservation(user, seat, today, "09:00", "11:00", ReservationStatus.ACTIVE));
    }

    // ---- 冲突检测 ----

    @Test
    void findConflicting_exactOverlap_returnsConflict() {
        List<Reservation> result = findConflicting("09:00", "11:00");
        assertThat(result).hasSize(1);
    }

    @Test
    void findConflicting_partialOverlapFromLeft_returnsConflict() {
        // 新预约 08:00–10:00，与已有的 09:00–11:00 重叠
        assertThat(findConflicting("08:00", "10:00")).hasSize(1);
    }

    @Test
    void findConflicting_partialOverlapFromRight_returnsConflict() {
        // 新预约 10:00–12:00
        assertThat(findConflicting("10:00", "12:00")).hasSize(1);
    }

    @Test
    void findConflicting_adjacent_returnsEmpty() {
        // 新预约 11:00–13:00，紧邻不重叠
        assertThat(findConflicting("11:00", "13:00")).isEmpty();
    }

    @Test
    void findConflicting_before_returnsEmpty() {
        // 新预约 07:00–09:00，紧邻不重叠
        assertThat(findConflicting("07:00", "09:00")).isEmpty();
    }

    @Test
    void findConflicting_cancelledReservation_returnsEmpty() {
        // 把已有预约取消后，同时段不再冲突
        Reservation cancelled = reservation(user, seat, today, "09:00", "11:00", ReservationStatus.CANCELLED);
        reservationRepository.save(cancelled);

        // 只有 CANCELLED，没有 ACTIVE 冲突
        List<Reservation> result = reservationRepository.findConflicting(
                seat.getId(), today,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                EXCLUDED);
        // 原来的 ACTIVE 还在，所以仍有 1 个冲突
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void findConflicting_differentSeat_returnsEmpty() {
        Library lib2 = libraryRepository.save(library());
        Seat otherSeat = seatRepository.save(seat(lib2));
        // 对另一个座位查询，不会冲突
        List<Reservation> result = reservationRepository.findConflicting(
                otherSeat.getId(), today,
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                EXCLUDED);
        assertThat(result).isEmpty();
    }

    // ---- 其他查询 ----

    @Test
    void findByUserIdAndDate_returnsUsersReservations() {
        List<Reservation> result = reservationRepository.findByUserIdAndDate(user.getId(), today);
        assertThat(result).hasSize(1);
    }

    @Test
    void findByStatusAndDate_returnsCorrectReservations() {
        reservationRepository.save(reservation(user, seat, today, "14:00", "16:00", ReservationStatus.CANCELLED));
        List<Reservation> active = reservationRepository.findByStatusAndDate(ReservationStatus.ACTIVE, today);
        assertThat(active).hasSize(1);
    }

    // ---- helpers ----

    private List<Reservation> findConflicting(String start, String end) {
        return reservationRepository.findConflicting(
                seat.getId(), today,
                LocalTime.parse(start), LocalTime.parse(end),
                EXCLUDED);
    }

    private Library library() {
        Library l = new Library();
        l.setName("测试馆");
        l.setAddress("测试地址");
        return l;
    }

    private Seat seat(Library lib) {
        Seat s = new Seat();
        s.setLibrary(lib);
        s.setSeatNo("2F-Q-001");
        s.setFloor((short) 2);
        s.setArea(SeatArea.QUIET);
        s.setQrToken("qr" + System.nanoTime());
        return s;
    }

    private User user() {
        User u = new User();
        u.setUserNo("T" + System.nanoTime());
        u.setRealName("测试用户");
        u.setPasswordHash("hash");
        u.setEmail(System.nanoTime() + "@test.com");
        return u;
    }

    private Reservation reservation(User u, Seat s, LocalDate date,
                                    String start, String end, ReservationStatus status) {
        Reservation r = new Reservation();
        r.setUser(u);
        r.setSeat(s);
        r.setDate(date);
        r.setStartTime(LocalTime.parse(start));
        r.setEndTime(LocalTime.parse(end));
        r.setStatus(status);
        return r;
    }
}
