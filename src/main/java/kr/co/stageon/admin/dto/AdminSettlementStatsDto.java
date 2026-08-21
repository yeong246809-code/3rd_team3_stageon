package kr.co.stageon.admin.dto;

import java.math.BigDecimal;

/** 정산·매출 관리 화면 상단 통계 카드입니다. */
public record AdminSettlementStatsDto(
        BigDecimal totalGrossAmount,
        BigDecimal totalRefundAmount,
        BigDecimal netRevenue,
        long paymentCount
) {
}