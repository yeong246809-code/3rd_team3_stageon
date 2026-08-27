package kr.co.stageon.admin.dto;

import java.time.LocalDateTime;

/**
 * 좌석 삭제 실패 시 "어느 회차에 배정되어 있는지" 안내하기 위한 DTO입니다.
 * AD06 홀 상세 "좌석 개별 삭제" 모달에서 사용합니다.
 */
public record ScheduleAssignmentInfoDto(
        Long seatId,
        String sectionName,
        String rowLabel,
        String seatNumber,
        Long scheduleId,
        Long performanceId,
        String performanceTitle,
        Integer roundNumber,
        LocalDateTime startsAt,
        String status
) {
}