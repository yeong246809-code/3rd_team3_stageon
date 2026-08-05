package kr.co.stageon.performance.domain;

import jakarta.persistence.*;
import kr.co.stageon.venue.domain.SeatChart;
import kr.co.stageon.venue.domain.VenueHall;
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
    @JoinColumn(name = "venue_hall_id", nullable = false)
    private VenueHall venueHall;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_chart_id", nullable = false)
    private SeatChart seatChart;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "sales_open_at", nullable = false)
    private LocalDateTime salesOpenAt;

    @Column(name = "sales_close_at", nullable = false)
    private LocalDateTime salesCloseAt;

    @Column(name = "cancel_close_at")
    private LocalDateTime cancelCloseAt;

    @Column(name = "max_tickets_per_member", nullable = false)
    private Integer maxTicketsPerMember;

    @Column(name = "seatsio_event_key", unique = true, length = 100)
    private String seatsioEventKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /** 관리자 "회차 추가" 화면에서 신규 회차를 생성할 때 사용합니다. */
    public static PerformanceSchedule create(Performance performance, VenueHall venueHall, SeatChart seatChart,
                                             Integer roundNumber, LocalDateTime startsAt,
                                             LocalDateTime salesOpenAt, LocalDateTime salesCloseAt,
                                             LocalDateTime cancelCloseAt, Integer maxTicketsPerMember,
                                             Status status) {
        PerformanceSchedule s = new PerformanceSchedule();
        s.performance = performance;
        s.venueHall = venueHall;
        s.seatChart = seatChart;
        s.roundNumber = roundNumber;
        s.startsAt = startsAt;
        s.salesOpenAt = salesOpenAt;
        s.salesCloseAt = salesCloseAt;
        s.cancelCloseAt = cancelCloseAt;
        s.maxTicketsPerMember = maxTicketsPerMember;
        s.status = status;
        return s;
    }

    /** 판매 상태를 변경합니다. 이미 취소된 회차는 되돌릴 수 없습니다. */
    public void changeStatus(Status newStatus) {
        if (this.status == Status.CANCELLED) {
            throw new IllegalStateException("이미 취소된 회차입니다. 상태를 변경할 수 없습니다.");
        }
        this.status = newStatus;
    }
}