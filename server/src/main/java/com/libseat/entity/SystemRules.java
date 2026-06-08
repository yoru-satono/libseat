package com.libseat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "system_rules")
@Getter
@Setter
@NoArgsConstructor
public class SystemRules {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    /** NULL 表示全局规则，非 NULL 表示该图书馆的覆盖规则 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id")
    private Library library;

    @Column(name = "open_time_start", nullable = false)
    private LocalTime openTimeStart = LocalTime.of(7, 0);

    @Column(name = "open_time_end", nullable = false)
    private LocalTime openTimeEnd = LocalTime.of(22, 0);

    @Column(name = "advance_days_max", nullable = false)
    private Short advanceDaysMax = 7;

    @Column(name = "single_min_minutes", nullable = false)
    private Short singleMinMinutes = 30;

    @Column(name = "single_max_hours", nullable = false)
    private Short singleMaxHours = 4;

    @Column(name = "daily_max_hours", nullable = false)
    private Short dailyMaxHours = 8;

    @Column(name = "checkin_early_minutes", nullable = false)
    private Short checkinEarlyMinutes = 10;

    @Column(name = "checkin_late_minutes", nullable = false)
    private Short checkinLateMinutes = 15;

    @Column(name = "no_show_threshold", nullable = false)
    private Short noShowThreshold = 3;

    @Column(name = "suspend_days", nullable = false)
    private Short suspendDays = 7;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
