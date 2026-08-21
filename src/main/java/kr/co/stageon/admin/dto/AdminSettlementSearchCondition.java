package kr.co.stageon.admin.dto;

import java.time.LocalDateTime;

/** AD 정산·매출 관리 화면의 검색 조건입니다. */
public record AdminSettlementSearchCondition(
        String keyword,
        LocalDateTime fromDate,
        LocalDateTime toDate
) {
}