package kr.co.stageon.admin.dto;

/** AD08 "좌석 구성 관리" 모달에서 아직 회차에 등록되지 않은 물리 좌석 한 칸입니다. */
public record SeatInventoryUnassignedSeatDto(
        Long seatId,
        String sectionName,
        String rowLabel,
        String seatNumber,
        String gradeName,
        String displayColor
) {
}