package kr.co.stageon.admin.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 공연장 구조 테이블 1행(공연장·홀·등급 구성·좌석도 상태)입니다. */
@Getter
@RequiredArgsConstructor
public class VenueStructureRowDto {
    private final Long venueId;
    private final String venueName;
    private final Long hallId;
    private final String hallName;
    /** 예: "VIP·R·S" 또는 등급이 없으면 "미설정", 홀이 없으면 "-" */
    private final String gradeSummary;
    /** 활성 좌석도가 있고 등급이 1개 이상이면 true(설정완료) */
    private final boolean configured;
}