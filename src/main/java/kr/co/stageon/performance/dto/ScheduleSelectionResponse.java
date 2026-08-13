package kr.co.stageon.performance.dto;

import kr.co.stageon.performance.domain.PerformanceSchedule;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [남수아 담당]
 * 날짜·회차 선택 화면 전용 DTO입니다.
 *
 * salesOpenAt / salesCloseAt을 화면으로 전달하여
 * 회차가 현재 시간에 맞춰 자동으로 열리고 닫히도록 합니다.
 */
public record ScheduleSelectionResponse(
        Long scheduleId,
        Long performanceId,
        String venueName,
        String venueHallName,
        Integer roundNumber,
        LocalDateTime startsAt,
        LocalDateTime salesOpenAt,
        LocalDateTime salesCloseAt,
        Integer maxTicketsPerMember,
        String status,
        List<SeatGradeAvailabilityResponse> seatGrades
) {

    public static ScheduleSelectionResponse from(
            PerformanceSchedule schedule,
            List<SeatGradeAvailabilityResponse> seatGrades
    ) {
        var venueHall = schedule.getVenueHall();

        return new ScheduleSelectionResponse(
                schedule.getId(),
                schedule.getPerformance().getId(),
                venueHall.getVenue().getName(),
                venueHall.getName(),
                schedule.getRoundNumber(),
                schedule.getStartsAt(),
                schedule.getSalesOpenAt(),
                schedule.getSalesCloseAt(),
                schedule.getMaxTicketsPerMember(),
                schedule.getStatus().name(),
                seatGrades
        );
    }

    public long totalAvailableSeatCount() {
        if (seatGrades == null) {
            return 0;
        }

        return seatGrades.stream()
                .mapToLong(
                        SeatGradeAvailabilityResponse::availableSeatCount
                )
                .sum();
    }
}