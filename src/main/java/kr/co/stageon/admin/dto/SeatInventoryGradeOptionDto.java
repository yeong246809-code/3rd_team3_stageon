package kr.co.stageon.admin.dto;

/** AD08 "새 좌석 추가" 폼의 등급 선택 드롭다운 옵션입니다. */
public record SeatInventoryGradeOptionDto(
        Long gradeId,
        String gradeName,
        String displayColor
) {
}