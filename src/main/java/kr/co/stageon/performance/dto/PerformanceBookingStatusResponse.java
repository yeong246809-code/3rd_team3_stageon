package kr.co.stageon.performance.dto;

import java.time.LocalDateTime;

/**
 * 공연 상세 페이지에서 사용자에게 보여줄
 * 현재 예매 상태를 전달하는 DTO
 */
public record PerformanceBookingStatusResponse(

        // 상태 코드
        String status,

        // 상단 작은 배지
        String statusLabel,

        // 카드 제목
        String title,

        // 안내 문구
        String description,

        // 버튼 문구
        String buttonText,

        // 실제 예매 가능 여부
        boolean bookable,

        // 티켓 오픈 예정 일시
        LocalDateTime nextOpenAt

) {

    /**
     * 티켓 오픈 전
     */
    public static PerformanceBookingStatusResponse upcoming(LocalDateTime nextOpenAt) {
        return new PerformanceBookingStatusResponse(
                "UPCOMING",
                "티켓 오픈 예정",
                "티켓 오픈 예정",
                "오픈 시간에 맞춰 다시 확인해 주세요.",
                "티켓 오픈 예정",
                false,
                nextOpenAt
        );
    }


    /**
     * 현재 예매 가능
     */
    public static PerformanceBookingStatusResponse available() {
        return new PerformanceBookingStatusResponse(
                "AVAILABLE",
                "예매 가능",
                "지금 예매할 수 있어요",
                "현재 예매 가능한 공연입니다.",
                "예매 일정 확인",
                true,
                null
        );
    }


    /**
     * 온라인 예매 마감
     */
    public static PerformanceBookingStatusResponse closed() {
        return new PerformanceBookingStatusResponse(
                "CLOSED",
                "예매 마감",
                "온라인 예매가 마감되었어요",
                "온라인 예매가 종료되었습니다.",
                "예매 마감",
                false,
                null
        );
    }


    /**
     * 공연 종료
     */
    public static PerformanceBookingStatusResponse ended() {
        return new PerformanceBookingStatusResponse(
                "ENDED",
                "공연 종료",
                "종료된 공연입니다",
                "공연이 종료되어 예매할 수 없습니다.",
                "공연 종료",
                false,
                null
        );
    }


    /**
     * 공연 취소
     */
    public static PerformanceBookingStatusResponse cancelled() {
        return new PerformanceBookingStatusResponse(
                "CANCELLED",
                "공연 취소",
                "취소된 공연입니다",
                "해당 공연의 예매가 취소되었습니다.",
                "공연 취소",
                false,
                null
        );
    }
}