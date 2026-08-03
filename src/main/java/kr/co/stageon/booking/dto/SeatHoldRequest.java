package kr.co.stageon.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** 좌석 선점 요청에 필요한 사용자·회차 식별자입니다. */
public record SeatHoldRequest(
        @NotNull @Positive Long memberId,
        @NotNull @Positive Long scheduleId,
        @NotEmpty List<Long> scheduleSeatIds
) {
}
