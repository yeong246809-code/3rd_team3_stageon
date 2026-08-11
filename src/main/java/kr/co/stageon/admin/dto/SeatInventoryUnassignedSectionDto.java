package kr.co.stageon.admin.dto;

import java.util.List;

/** AD08 "좌석 구성 관리" 모달의 미등록 좌석을 구역별로 묶은 DTO입니다. */
public record SeatInventoryUnassignedSectionDto(
        String sectionName,
        List<SeatInventoryUnassignedSeatDto> seats
) {
}