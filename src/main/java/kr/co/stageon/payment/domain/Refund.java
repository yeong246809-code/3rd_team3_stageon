package kr.co.stageon.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 결제 취소(환불) 이력을 기록합니다. */
@Getter
@Entity
@Table(name = "refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund {

    public enum Status { REQUESTED, COMPLETED, FAILED }
    public enum Category { ADMIN_CANCEL, USER_CANCEL, SYSTEM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_category", nullable = false, length = 30)
    private Category refundCategory;

    @Column(length = 200)
    private String reason;

    @Column(name = "pg_tid", length = 100)
    private String pgTid;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 관리자 강제취소 시 환불 이력을 즉시 완료 처리로 생성합니다(모의 결제이므로 실제 PG 연동 없음). */
    public static Refund createCompleted(Payment payment, BigDecimal amount, Category category, String reason) {
        Refund refund = new Refund();
        refund.payment = payment;
        refund.amount = amount;
        refund.status = Status.COMPLETED;
        refund.refundCategory = category;
        refund.reason = reason;
        refund.requestedAt = LocalDateTime.now();
        refund.processedAt = LocalDateTime.now();
        return refund;
    }
}