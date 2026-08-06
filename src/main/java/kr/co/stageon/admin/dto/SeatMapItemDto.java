package kr.co.stageon.admin.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 공연 등록 화면의 좌석 배치도 미리보기용 개별 좌석 DTO입니다. */
@Getter
@RequiredArgsConstructor
public class SeatMapItemDto {
    private final Long id;
    private final String sectionName;
    private final String rowLabel;
    private final String seatNumber;
    private final Long gradeId;
    private final String gradeName;
    private final String displayColor;
}