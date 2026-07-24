package kr.co.stageon.performance.domain;

import jakarta.persistence.*;
import kr.co.stageon.venue.domain.Venue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 공연의 날짜·시간별 회차와 판매 가능 시간을 나타냅니다. */
@Getter
@Entity
@Table(name = "performance_schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerformanceSchedule {

    public enum Status { SCHEDULED, OPEN, CLOSED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "sales_open_at", nullable = false)
    private LocalDateTime salesOpenAt;

    @Column(name = "sales_close_at", nullable = false)
    private LocalDateTime salesCloseAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
