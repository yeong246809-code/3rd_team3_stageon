package kr.co.stageon.performance.dto;

import kr.co.stageon.performance.domain.PerformanceSchedule;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [남수아 담당]
 * 날짜·회차 선택 화면 전용 DTO입니다.
 *
 * 기존 ScheduleResponse는 사용하거나 수정하지 않습니다.
 */

public record ScheduleSelectionResponse(
        Long scheduleId,
        Long performanceId,
        String venueName,
        String venueHallName,
        Integer roundNumber,
        LocalDateTime startsAt,
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
