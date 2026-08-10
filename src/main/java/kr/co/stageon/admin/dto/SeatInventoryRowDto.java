package kr.co.stageon.admin.dto;

import java.util.List;

/** AD08 좌석 배치도의 한 행(row)입니다. */
public record SeatInventoryRowDto(
        String rowLabel,
        List<SeatInventorySeatDto> seats
) {
}