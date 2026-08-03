package kr.co.stageon.admin.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 홀의 활성 좌석도 기준 좌석 등급·색상·좌석수 요약 DTO입니다. */
@Getter
@RequiredArgsConstructor
public class SeatGradeSummaryDto {
    private final Long id;
    private final String name;
    private final String displayColor;
    private final long seatCount;
}