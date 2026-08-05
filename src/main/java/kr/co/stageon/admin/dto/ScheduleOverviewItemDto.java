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
    /** 같은 공연장 홀에서 시간대가 겹치는 회차인지 여부입니다. */
    private final boolean conflict;
}