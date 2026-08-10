package kr.co.stageon.admin.dto;

/** AD08 "활성 선점" 테이블 한 행입니다. */
public record SeatInventoryHoldDto(
        Long holdId,
        String memberLabel,
        int seatCount,
        String expiresAtIso
) {
}