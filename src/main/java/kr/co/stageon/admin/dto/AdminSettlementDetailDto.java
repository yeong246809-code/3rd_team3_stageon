package kr.co.stageon.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 정산·매출 관리 상세 모달 - 공연 1건의 회차별 매출 breakdown 및 예매자 목록입니다. */
public record AdminSettlementDetailDto(
        Long performanceId,
        String title,
        BigDecimal totalGrossAmount,
        BigDecimal totalRefundAmount,
        BigDecimal totalNetAmount,
        List<ScheduleItem> schedules,
        List<BookerItem> bookers
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

    /** 해당 공연을 예매한 회원 1건입니다. AD09 예매·주문 조회로 바로 넘어갈 수 있도록 예매번호를 함께 내려줍니다. */
    public record BookerItem(
            String bookingNumber,
            String maskedMemberName,
            int seatCount,
            BigDecimal totalAmount,
            LocalDateTime reservedAt
    ) {
    }
}