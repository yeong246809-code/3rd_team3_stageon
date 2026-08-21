package kr.co.stageon.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 정산·매출 관리 상세 모달 - 공연 1건의 회차별 매출 breakdown입니다. */
public record AdminSettlementDetailDto(
        Long performanceId,
        String title,
        BigDecimal totalGrossAmount,
        BigDecimal totalRefundAmount,
        BigDecimal totalNetAmount,
        List<ScheduleItem> schedules
) {
    public record ScheduleItem(
            Long scheduleId,
            LocalDateTime startsAt,
            long paymentCount,
            BigDecimal grossAmount,
            BigDecimal refundAmount,
            BigDecimal netAmount
    ) {
    }
}