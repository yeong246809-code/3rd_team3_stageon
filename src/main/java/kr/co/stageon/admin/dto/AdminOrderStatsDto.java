package kr.co.stageon.admin.dto;

import java.math.BigDecimal;

/** AD09 상단 통계 카드입니다. */
public record AdminOrderStatsDto(
        long todayReservationCount,
        BigDecimal totalRevenue,
        long cancelledCount
) {
}