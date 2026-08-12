package kr.co.stageon.booking.domain;

import jakarta.persistence.*;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 좌석 선점 이후 결제 결과에 따라 확정·취소·만료되는 예매입니다. */
@Getter
@Entity
@Table(name = "reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    public enum Status { PENDING, RESERVED, CANCELLED, EXPIRED }
    public enum ReceiveMethod { MOBILE, ONSITE } //티켓수령방법 추가

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_number", nullable = false, unique = true, length = 30)
    private String bookingNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private PerformanceSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_hold_id")
    private SeatHold seatHold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "receive_method", nullable = false, length = 20)
    private ReceiveMethod receiveMethod;

    @Column(name = "seat_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal seatAmount;

    @Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public static Reservation create(String bookingNumber, Member member, PerformanceSchedule schedule,
                                     SeatHold seatHold, ReceiveMethod receiveMethod, BigDecimal seatAmount, BigDecimal totalAmount) {
        Reservation reservation = new Reservation();
        reservation.bookingNumber = bookingNumber;
        reservation.member = member;
        reservation.schedule = schedule;
        reservation.seatHold = seatHold;
        reservation.status = Status.RESERVED;
        reservation.receiveMethod = receiveMethod;
        reservation.seatAmount = seatAmount;
        reservation.feeAmount = BigDecimal.ZERO; // 수수료 면제 가정
        reservation.discountAmount = BigDecimal.ZERO; // 할인 없음 가정
        reservation.totalAmount = totalAmount;
        reservation.expiresAt = LocalDateTime.now().plusDays(1);
        reservation.reservedAt = LocalDateTime.now();
        return reservation;
    }

    /** AD09 관리자 화면에서 확정된 예매를 강제로 취소 처리할 때 사용합니다. */
    public void cancel(String reason) {
        if (this.status == Status.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예매입니다.");
        }
        this.status = Status.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = reason;
    }
}