package kr.co.stageon.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kr.co.stageon.performance.domain.PerformanceSchedule;

import java.time.LocalDateTime;

/** 관리자 회차 등록·수정 화면의 입력 DTO입니다. */
public record AdminScheduleRequest(
        @NotNull @Positive Long performanceId,
        @NotNull @Positive Long venueHallId,
        @NotNull @Positive Long seatChartId,
        @Positive Integer roundNumber,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime salesOpenAt,
        @NotNull LocalDateTime salesCloseAt,
        LocalDateTime cancelCloseAt,
        @NotNull @Positive Integer maxTicketsPerMember,
        @NotNull PerformanceSchedule.Status status
) {
}
