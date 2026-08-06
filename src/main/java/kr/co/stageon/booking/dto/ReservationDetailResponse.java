package kr.co.stageon.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 마이페이지 예매 상세 화면에 필요한 정보를 한 번에 전달합니다.
 * 기존 예매 요약 DTO와 분리해 다른 팀원의 목록·결제 코드를 최소한으로 건드립니다.
 */
public record ReservationDetailResponse(
        Long reservationId,
        String bookingNumber,
        Long memberId,
        String performanceTitle,
        String posterUrl,
        LocalDateTime startsAt,
        Integer roundNumber,
        String venueName,
        String hallName,
        String venueAddress,
        String reservationStatus,
        BigDecimal seatAmount,
        BigDecimal feeAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        LocalDateTime reservedAt,
        LocalDateTime cancelledAt,
        String cancelReason,
        String paymentProvider,
        String paymentStatus,
        LocalDateTime paymentRequestedAt,
        LocalDateTime paymentProcessedAt,
        List<ReservedSeatItem> seats
) {

    /** 예매 당시 저장된 좌석 스냅샷입니다. */
    public record ReservedSeatItem(
            String gradeName,
            String sectionName,
            String rowLabel,
            String seatNumber,
            BigDecimal unitPrice
    ) {
    }
}