package kr.co.stageon.performance.dto;

/** 회차 선택 화면에 표시할 좌석 등급별 잔여석 정보입니다. */
public record SeatGradeAvailabilityResponse(
        Long gradeId,
        String gradeName,
        String displayColor,
        Integer sortOrder,
        long totalSeatCount,
        long availableSeatCount
) {
}