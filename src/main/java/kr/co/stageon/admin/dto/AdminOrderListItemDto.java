package kr.co.stageon.admin.dto;

import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.payment.domain.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** AD09 예매·주문 목록 테이블 한 행입니다. */
public record AdminOrderListItemDto(
        Long reservationId,
        String bookingNumber,
        String maskedMemberName,
        String performanceTitle,
        LocalDateTime scheduleStartsAt,
        int seatCount,
        BigDecimal totalAmount,
        Payment.Status paymentStatus,
        Reservation.Status reservationStatus,
        LocalDateTime createdAt
) {
}