package kr.co.stageon.admin.dto;

import java.math.BigDecimal;

/** AD08 좌석 재고 현황 화면의 등급별 잔여석 테이블용 DTO입니다. */
public record SeatInventoryGradeDto(
        Long gradeId,
        String gradeName,
        String displayColor,
        BigDecimal price,
        long total,
        long available,
        long held,
        long reserved,
        long blocked
) {
}