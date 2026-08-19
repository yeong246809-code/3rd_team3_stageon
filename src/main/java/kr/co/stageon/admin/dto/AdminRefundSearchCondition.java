package kr.co.stageon.admin.dto;

import kr.co.stageon.payment.domain.Refund;

import java.time.LocalDateTime;

/** 환불 관리 화면 검색 조건입니다. */
public record AdminRefundSearchCondition(
        Refund.Status status,
        Refund.Category category,
        String keyword,
        LocalDateTime fromDate,
        LocalDateTime toDate
) {
}