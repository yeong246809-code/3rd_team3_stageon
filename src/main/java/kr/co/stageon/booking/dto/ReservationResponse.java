package kr.co.stageon.booking.dto;

import kr.co.stageon.booking.domain.Reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 예매 완료와 마이페이지에서 사용하는 예매 요약 DTO입니다. */
public record ReservationResponse(
        Long id,
        String bookingNumber,
        Long memberId,
        Long scheduleId,
        String performanceTitle,
        LocalDateTime startsAt,
        String status,
        BigDecimal totalAmount,
        LocalDateTime expiresAt,
        LocalDateTime reservedAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getBookingNumber(),
                reservation.getMember().getId(),
                reservation.getSchedule().getId(),
                reservation.getSchedule().getPerformance().getTitle(),
                reservation.getSchedule().getStartsAt(),
                reservation.getStatus().name(),
                reservation.getTotalAmount(),
                reservation.getExpiresAt(),
                reservation.getReservedAt()
        );
    }
}
