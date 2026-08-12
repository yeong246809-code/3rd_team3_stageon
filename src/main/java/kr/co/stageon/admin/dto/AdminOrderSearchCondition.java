package kr.co.stageon.admin.dto;

import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.payment.domain.Payment;

import java.time.LocalDateTime;

/** AD09 "예매·주문 조회" 검색 조건입니다. */
public record AdminOrderSearchCondition(
        Long performanceId,
        Long scheduleId,
        Reservation.Status status,
        Payment.Status paymentStatus,
        String keyword,
        LocalDateTime fromDate,
        LocalDateTime toDate
) {
}