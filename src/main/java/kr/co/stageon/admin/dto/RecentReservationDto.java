package kr.co.stageon.admin.dto;

/** 대시보드 '최근 예매' 테이블 한 행을 담는 DTO입니다. */
public record RecentReservationDto(
        String bookingNumber,
        String performanceTitle,
        String status
) {
}