package kr.co.stageon.admin.dto;

/** 정산·매출 관리 화면의 "공연명 검색" 모달에 노출할 공연 옵션입니다. */
public record AdminPerformanceOptionDto(
        Long id,
        String title
) {
}