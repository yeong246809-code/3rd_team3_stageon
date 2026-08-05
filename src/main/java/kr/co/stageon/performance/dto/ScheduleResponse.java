package kr.co.stageon.performance.dto;

import kr.co.stageon.performance.domain.PerformanceSchedule;

import java.time.LocalDateTime;


/** 회차 선택 화면과 회차 API에서 사용하는 DTO입니다. */
public record ScheduleResponse(
        Long id,
        Long performanceId,
        Long venueId,
        String venueName,
        Long venueHallId,
        String venueHallName,
        Long seatChartId,
        Integer roundNumber,
        LocalDateTime startsAt,
        LocalDateTime salesOpenAt,
        LocalDateTime salesCloseAt,
        LocalDateTime cancelCloseAt,
        Integer maxTicketsPerMember,
        String seatsioEventKey,
        String status

) {
    public static ScheduleResponse from(PerformanceSchedule schedule) {
        var venueHall = schedule.getVenueHall();
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getPerformance().getId(),
                venueHall.getVenue().getId(),
                venueHall.getVenue().getName(),
                venueHall.getId(),
                venueHall.getName(),
                schedule.getSeatChart().getId(),
                schedule.getRoundNumber(),
                schedule.getStartsAt(),
                schedule.getSalesOpenAt(),
                schedule.getSalesCloseAt(),
                schedule.getCancelCloseAt(),
                schedule.getMaxTicketsPerMember(),
                schedule.getSeatsioEventKey(),
                schedule.getStatus().name()
        );
    }
}
