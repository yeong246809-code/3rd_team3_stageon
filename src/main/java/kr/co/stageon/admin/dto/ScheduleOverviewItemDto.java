package kr.co.stageon.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 상단 "회차 운영 현황" 요약 테이블의 한 행입니다. */
@Getter
@AllArgsConstructor
public class ScheduleOverviewItemDto {
    private final Long id;
    private final String performanceTitle;
    private final String dateTimeText;
    private final String statusText;
    private final String badgeClass;
}