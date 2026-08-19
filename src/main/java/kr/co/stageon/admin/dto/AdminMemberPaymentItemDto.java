package kr.co.stageon.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 수동 환불 모달에서 회원 선택 후 보여줄 환불 가능 결제 건 한 건입니다. */
public record AdminMemberPaymentItemDto(
        Long paymentId,
        String bookingNumber,
        String performanceTitle,
        BigDecimal amount,
        BigDecimal cancelAmount,
        BigDecimal refundableAmount,
        LocalDateTime requestedAt
) {
}