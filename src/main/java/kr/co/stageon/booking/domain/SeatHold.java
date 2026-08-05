package kr.co.stageon.booking.domain;

import jakarta.persistence.*;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 한 회원이 한 회차에서 임시로 선점한 좌석 묶음입니다. */
@Getter
@Entity
@Table(
        name = "seat_holds",
        indexes = @Index(name = "idx_seat_hold_expiration", columnList = "status,expires_at")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatHold {

    public enum Status { ACTIVE, BOOKED, RELEASED, EXPIRED, CANCELLED }
    //활성, 예매완료, 결제오류, 만료, 결제취소

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private PerformanceSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "hold_token_hash", nullable = false, unique = true, length = 64)
    private String holdTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public void expire() {
        this.status = Status.EXPIRED;
        this.releasedAt = LocalDateTime.now();
    }

    public static SeatHold create(Member member, PerformanceSchedule schedule, LocalDateTime expiresAt, String holdTokenHash) {
        SeatHold seatHold = new SeatHold();
        seatHold.member = member;
        seatHold.schedule = schedule;
        seatHold.status = Status.ACTIVE;
        seatHold.expiresAt = expiresAt;
        seatHold.holdTokenHash = holdTokenHash;
        seatHold.startedAt = LocalDateTime.now();
        return seatHold;
    }

    public void cancel() {
        this.status = Status.CANCELLED;
        this.expiresAt = LocalDateTime.now(); // 즉시 만료 처리
    }

}
