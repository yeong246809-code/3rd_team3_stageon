package kr.co.stageon.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 마이페이지 > 보유 티켓 화면 전용 DTO
 * 좌석 1개를 모바일 티켓 1장으로 표현합니다.
 */
public record MyTicketResponse(

        Long reservationSeatId,
        Long reservationId,
        String bookingNumber,

        String performanceTitle,
        String posterUrl,
        LocalDateTime startsAt,

        String venueName,
        String hallName,

        String gradeName,
        String sectionName,
        String rowLabel,
        String seatNumber,
        BigDecimal unitPrice,

        // AVAILABLE / ENDED / TRANSFERRED
        String ticketStatus,

        // QR 코드 이미지
        String qrCodeImage

) {
}