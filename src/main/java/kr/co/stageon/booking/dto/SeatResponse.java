package kr.co.stageon.booking.dto;

import kr.co.stageon.booking.domain.ScheduleSeat;

import java.math.BigDecimal;

/** 좌석 배치 화면에서 공개 가능한 회차 좌석 정보입니다. */
public record SeatResponse(
        Long id,
        Long scheduleId,
        String section,
        String row,
        String number,
        String grade,
        String displayColor,
        String seatsioObjectKey,
        String objectType,
        BigDecimal price,
        String currency,
        String status,
        boolean accessible
) {
    public static SeatResponse from(ScheduleSeat scheduleSeat) {
        var seat = scheduleSeat.getSeat();
        return new SeatResponse(
                scheduleSeat.getId(),
                scheduleSeat.getSchedule().getId(),
                seat.getSectionName(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getSeatGrade().getName(),
                seat.getSeatGrade().getDisplayColor(),
                seat.getSeatsioObjectKey(),
                seat.getObjectType().name(),
                scheduleSeat.getPrice(),
                scheduleSeat.getCurrency(),
                scheduleSeat.getStatus().name(),
                seat.isAccessible()
        );
    }
}
