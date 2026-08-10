package kr.co.stageon.admin.dto;

import java.math.BigDecimal;

/** AD08 좌석 배치도의 좌석 한 칸을 나타내는 DTO입니다. */
public record SeatInventorySeatDto(
        Long scheduleSeatId,
        String seatNumber,
        String status,
        String statusText,
        String gradeName,
        String displayColor,
        BigDecimal price
) {
}