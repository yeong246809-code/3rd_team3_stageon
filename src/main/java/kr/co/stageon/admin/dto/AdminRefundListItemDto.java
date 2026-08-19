package kr.co.stageon.admin.dto;

import kr.co.stageon.payment.domain.Refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 환불 관리 목록 한 행입니다. */
public record AdminRefundListItemDto(
        Long refundId,
        String bookingNumber,
        String maskedMemberName,
        String performanceTitle,
        BigDecimal amount,
        Refund.Status status,
        Refund.Category category,
        String reason,
        LocalDateTime requestedAt,
        LocalDateTime processedAt
) {
}