package kr.co.stageon.booking.dto;

import kr.co.stageon.booking.domain.ScheduleSeat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 좌석 배치 화면에서 공개 가능한 회차 좌석 정보입니다. */
public record SeatResponse(
        Long id,
        Long scheduleId,
        String section,
        String row,
        String number,
        String grade,
        String displayColor,
        BigDecimal price,
        String status,
        boolean accessible,
        LocalDateTime holdExpiresAt
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
                scheduleSeat.getPrice(),
                scheduleSeat.getStatus().name(),
                seat.isAccessible(),
                scheduleSeat.getHoldExpiresAt()
        );
    }
}
