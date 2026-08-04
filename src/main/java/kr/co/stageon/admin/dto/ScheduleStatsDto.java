package kr.co.stageon.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 일정·회차 관리 화면 상단 통계 카드 3개(운영 회차/예정 회차/일정 충돌)를 담습니다. */
@Getter
@AllArgsConstructor
public class ScheduleStatsDto {
    private final long operatingCount;
    private final long upcomingCount;
    private final long conflictCount;
}