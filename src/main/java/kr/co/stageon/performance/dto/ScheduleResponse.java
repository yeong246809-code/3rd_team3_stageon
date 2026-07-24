package kr.co.stageon.performance.dto;

import kr.co.stageon.performance.domain.PerformanceSchedule;

import java.time.LocalDateTime;

/** 회차 선택 화면과 회차 API에서 사용하는 DTO입니다. */
public record ScheduleResponse(
        Long id,
        Long performanceId,
        Long venueId,
        String venueName,
        LocalDateTime startsAt,
        LocalDateTime salesOpenAt,
        LocalDateTime salesCloseAt,
        String status
) {
    public static ScheduleResponse from(PerformanceSchedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getPerformance().getId(),
                schedule.getVenue().getId(),
                schedule.getVenue().getName(),
                schedule.getStartsAt(),
                schedule.getSalesOpenAt(),
                schedule.getSalesCloseAt(),
                schedule.getStatus().name()
        );
    }
}
