package kr.co.stageon.admin.dto;

/** AD08 좌석 재고 현황 화면의 상단 통계 카드용 DTO입니다. */
public record SeatInventoryStatsDto(
        long total,
        long available,
        long held,
        long reserved,
        long blocked
) {
    public static SeatInventoryStatsDto empty() {
        return new SeatInventoryStatsDto(0, 0, 0, 0, 0);
    }
}