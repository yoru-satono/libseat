package com.libseat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "seat_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @Column(name = "seat_no", nullable = false, length = 20)
    private String seatNo;

    @Column(nullable = false)
    private Short floor;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "seat_area")
    private SeatArea area;

    @Column(name = "has_computer", nullable = false)
    private Boolean hasComputer = false;

    @Column(name = "has_power", nullable = false)
    private Boolean hasPower = false;

    @Column(name = "has_window", nullable = false)
    private Boolean hasWindow = false;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "seat_status_t")
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(name = "pos_x", precision = 7, scale = 2)
    private BigDecimal posX;

    @Column(name = "pos_y", precision = 7, scale = 2)
    private BigDecimal posY;

    @Column(name = "qr_token", unique = true, length = 64)
    private String qrToken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
