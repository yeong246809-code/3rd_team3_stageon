package kr.co.stageon.admin.dto;

import java.time.LocalDate;

/** 감사 로그 목록 화면의 검색 조건. 값이 없으면(null/blank) 해당 조건은 무시됩니다.
 *  관리자 계정이 1개뿐이라 관리자 이메일 검색 조건은 제거함(2026-08-25). */
public record AdminActivityLogSearchDto(
        String actionType,
        String targetEntity,
        LocalDate dateFrom,
        LocalDate dateTo
) {
}