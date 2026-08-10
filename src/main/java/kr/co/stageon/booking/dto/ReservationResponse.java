package kr.co.stageon.booking.dto;

import kr.co.stageon.booking.domain.Reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 예매 완료와 마이페이지에서 사용하는 예매 요약 DTO입니다.
 *
 * 마이페이지의 예매 내역뿐만 아니라
 * '다가오는 공연' 카드에서도 사용할 수 있도록
 * 공연 제목, 포스터, 공연 시작 일시를 함께 전달합니다.
 */
public record ReservationResponse(

        Long id,

        // 사용자에게 보여주는 예매번호
        String bookingNumber,

        // 예매한 회원 번호
        Long memberId,

        // 예매한 공연 회차 번호
        Long scheduleId,

        // 공연 제목
        String performanceTitle,

        // 공연 포스터 URL
        // Performance 엔티티의 posterUrl 값을 전달합니다.
        String posterUrl,

        // 실제 공연 시작 일시
        LocalDateTime startsAt,

        // 예매 상태
        // PENDING, RESERVED, CANCELLED, EXPIRED 등
        String status,

        // 티켓 수령 방식
        String receiveMethod,

        // 좌석 금액 합계
        BigDecimal seatAmount,

        // 예매 수수료
        BigDecimal feeAmount,

        // 할인 금액
        BigDecimal discountAmount,

        // 최종 결제 금액
        BigDecimal totalAmount,

        // 결제 대기 만료 시간
        LocalDateTime expiresAt,

        // 예매 확정 시간
        LocalDateTime reservedAt,

        // 예매 취소 시간
        LocalDateTime cancelledAt,

        // 취소 사유
        String cancelReason

) {

    /**
     * Reservation Entity를
     * 마이페이지에서 사용할 ReservationResponse DTO로 변환합니다.
     */
    public static ReservationResponse from(Reservation reservation) {

        return new ReservationResponse(

                reservation.getId(),

                reservation.getBookingNumber(),

                reservation.getMember().getId(),

                reservation.getSchedule().getId(),

                // 공연 제목
                reservation.getSchedule()
                        .getPerformance()
                        .getTitle(),

                // =====================================================
                // 마이페이지 '다가오는 공연' 카드용 포스터
                //
                // Reservation
                //   → PerformanceSchedule
                //   → Performance
                //   → posterUrl
                //
                // 관리자에서 공연 등록 시 저장한 poster_url이
                // 최종적으로 이 값으로 들어옵니다.
                // =====================================================
                reservation.getSchedule()
                        .getPerformance()
                        .getPosterUrl(),

                // 공연 시작 일시
                reservation.getSchedule()
                        .getStartsAt(),

                reservation.getStatus().name(),

                reservation.getReceiveMethod().name(),

                reservation.getSeatAmount(),

                reservation.getFeeAmount(),

                reservation.getDiscountAmount(),

                reservation.getTotalAmount(),

                reservation.getExpiresAt(),

                reservation.getReservedAt(),

                reservation.getCancelledAt(),

                reservation.getCancelReason()
        );
    }
}