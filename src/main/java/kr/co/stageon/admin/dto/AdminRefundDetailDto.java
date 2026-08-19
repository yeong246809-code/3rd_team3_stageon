package kr.co.stageon.admin.dto;

import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.payment.domain.Refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 환불 관리 상세 모달 데이터입니다. */
public record AdminRefundDetailDto(
        Long refundId,
        String bookingNumber,
        String maskedMemberName,
        String performanceTitle,
        LocalDateTime scheduleStartsAt,
        Long paymentId,
        Payment.PayMethod payMethod,
        BigDecimal paymentAmount,
        BigDecimal paymentCancelAmount,
        BigDecimal amount,
        Refund.Status status,
        Refund.Category category,
        String reason,
        String pgTid,
        LocalDateTime requestedAt,
        LocalDateTime processedAt
) {
}