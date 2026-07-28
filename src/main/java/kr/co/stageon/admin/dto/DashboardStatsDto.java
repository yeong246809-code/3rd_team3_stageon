package kr.co.stageon.admin.dto;

import java.util.List;

/** 관리자 대시보드 상단 통계 카드 + 최근 예매 목록 + 외부 연동 상태를 담는 DTO입니다. */
public record DashboardStatsDto(
        long todayReservationCount,
        long waitingMemberCount,
        long heldSeatCount,
        double paymentSuccessRate,
        List<RecentReservationDto> recentReservations,
        boolean ollamaConnected
) {
}