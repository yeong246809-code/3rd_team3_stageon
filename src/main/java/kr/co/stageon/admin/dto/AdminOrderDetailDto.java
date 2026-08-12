package kr.co.stageon.admin.dto;

import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.payment.domain.Refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** AD09 예매 상세 모달 데이터입니다. */
public record AdminOrderDetailDto(
        Long reservationId,
        String bookingNumber,
        String maskedMemberName,
        String maskedMemberPhone,
        String performanceTitle,
        LocalDateTime scheduleStartsAt,
        Reservation.ReceiveMethod receiveMethod,
        Reservation.Status status,
        BigDecimal seatAmount,
        BigDecimal feeAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        LocalDateTime reservedAt,
        LocalDateTime cancelledAt,
        String cancelReason,
        List<SeatItem> seats,
        List<PaymentItem> payments
) {
    public record SeatItem(
            String sectionName,
            String rowLabel,
            String seatNumber,
            String gradeName,
            BigDecimal unitPrice
    ) {
    }

    public record PaymentItem(
            Long paymentId,
            Payment.Provider provider,
            Payment.PayMethod payMethod,
            BigDecimal amount,
            BigDecimal cancelAmount,
            Payment.Status status,
            LocalDateTime requestedAt,
            LocalDateTime processedAt,
            List<RefundItem> refunds
    ) {
    }

    public record RefundItem(
            BigDecimal amount,
            Refund.Status status,
            Refund.Category category,
            String reason,
            LocalDateTime processedAt
    ) {
    }
}