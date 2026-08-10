package kr.co.stageon.booking.domain;

import jakarta.persistence.*;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import kr.co.stageon.venue.domain.Seat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 특정 회차에서 판매되는 좌석 재고입니다.
 * version 컬럼으로 동시 갱신 충돌을 감지하고 최종 중복 예약을 방지합니다.
 */
@Getter
@Entity
@Table(
        name = "schedule_seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_schedule_seat",
                columnNames = {"schedule_id", "seat_id"}
        ),
        indexes = @Index(name = "idx_schedule_seat_status", columnList = "schedule_id,status")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleSeat {

    public enum Status { AVAILABLE, HELD, RESERVED, BLOCKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private PerformanceSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /** AD08 "좌석 생성" - 회차에 아직 없는 물리 좌석을 회차 좌석 재고로 등록할 때 사용합니다. */
    public static ScheduleSeat create(PerformanceSchedule schedule, Seat seat, BigDecimal price, String currency) {
        ScheduleSeat ss = new ScheduleSeat();
        ss.schedule = schedule;
        ss.seat = seat;
        ss.price = price;
        ss.currency = currency;
        ss.status = Status.AVAILABLE;
        return ss;
    }

    public void hold() {
        if (this.status != Status.AVAILABLE) {
            throw new IllegalStateException("이미 선택되었거나 선점된 좌석입니다.");
        }
        this.status = Status.HELD;
    }

    public void release() {
        this.status = Status.AVAILABLE;
    }

    /** 결제 완료 시 좌석을 최종 판매완료 상태로 전환합니다. HELD 상태에서만 예매 확정할 수 있습니다. */
    public void reserve() {
        if (this.status != Status.HELD) {
            throw new IllegalStateException("선점되지 않은 좌석은 예매 확정할 수 없습니다.");
        }
        this.status = Status.RESERVED;
    }

    /**
     * AD08 관리자 화면에서 좌석 상태를 강제로 변경합니다.
     * 일반 예매 흐름(hold/release/reserve)과 달리 상태 전이 검증 없이 관리자가 직접 지정한 상태로 덮어씁니다.
     */
    public void forceStatus(Status status) {
        this.status = status;
    }
}