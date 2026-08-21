package kr.co.stageon.admin.dto;

import java.math.BigDecimal;

/** 정산·매출 관리 화면의 공연별 매출 목록 한 행입니다. */
public record AdminSettlementListItemDto(
        Long performanceId,
        String title,
        long paymentCount,
        BigDecimal grossAmount,
        BigDecimal refundAmount,
        BigDecimal netAmount
) {
}