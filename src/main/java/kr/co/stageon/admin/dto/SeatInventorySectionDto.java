package kr.co.stageon.admin.dto;

import java.util.List;

/** AD08 좌석 배치도의 한 구역(section)입니다. */
public record SeatInventorySectionDto(
        String sectionName,
        List<SeatInventoryRowDto> rows
) {
}