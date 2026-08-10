package kr.co.stageon.booking.dto;

import java.math.BigDecimal;

/**
 * 결제 화면에 필요한 총 결제 금액과 공연 정보를 전달하는 DTO입니다.
 */
public record PaymentSummaryDto(
        BigDecimal totalAmount,
        String performanceTitle
) {
}