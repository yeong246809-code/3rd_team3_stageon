package kr.co.stageon.admin.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** 공연장·좌석 관리 화면 상단 통계 + 공연장 구조 테이블용 DTO입니다. */
@Getter
@RequiredArgsConstructor
public class VenueDashboardDto {
    private final int venueCount;
    private final int hallCount;
    private final long totalSeatCount;
    private final List<VenueStructureRowDto> rows;
}