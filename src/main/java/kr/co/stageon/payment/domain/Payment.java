package kr.co.stageon.payment.domain;

import jakarta.persistence.*;
import kr.co.stageon.booking.domain.Reservation;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 동일 멱등키의 결제 요청이 한 번만 처리되도록 기록하는 모의 결제입니다. */
@Getter
@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_provider_idempotency",
                columnNames = {"provider", "idempotency_key"}
        ),
        indexes = @Index(name = "idx_payment_reservation_status", columnList = "reservation_id,status")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    public enum Status { READY, SUCCESS, FAILED, CANCELLED }
    public enum Provider { TOSSPAYMENTS }
    public enum PayMethod { CARD, VBANK, BANK, MOBILE } //결제수단 - 카드, 가상계좌, 무통장입금, 모바일

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Provider provider;

    @Column(name = "payment_key", nullable = false, length = 200)
    private String paymentKey;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    // 가상계좌 필드 추가 (nullable)
    @Column(name = "vbank_num", length = 50)
    private String vbankNum;

    //결제수단
    @Enumerated(EnumType.STRING)
    @Column(name = "pay_method", nullable = false, length = 30)
    private PayMethod payMethod;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // 환불 누적 금액
    @Column(name = "cancel_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal cancelAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Payment(Reservation reservation, String paymentKey, String orderId, Provider provider,
                   PayMethod payMethod, BigDecimal amount, Status status,
                   LocalDateTime requestedAt, LocalDateTime processedAt) {
        this.reservation = reservation;
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.provider = provider;
        this.payMethod = payMethod;
        this.amount = amount;
        this.status = status;
        this.requestedAt = requestedAt;
        this.processedAt = processedAt;
        this.cancelAmount = BigDecimal.ZERO;
    }

    /** AD09 관리자 강제취소 시 결제를 취소 상태로 전환하고 환불 누적액을 기록합니다. */
    public void markCancelled(BigDecimal refundAmount) {
        if (this.status != Status.SUCCESS) {
            throw new IllegalStateException("성공한 결제만 취소할 수 있습니다.");
        }
        this.status = Status.CANCELLED;
        this.cancelAmount = this.cancelAmount.add(refundAmount);
        this.processedAt = LocalDateTime.now();
    }
}