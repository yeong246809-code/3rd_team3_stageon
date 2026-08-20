package kr.co.stageon.admin.dto;

import java.time.LocalDate;

/** 상세보기 모달용 JSON 응답 DTO입니다. (AD09 예매 상세 모달과 동일한 fetch 패턴) */
public record VersionDetailDto(
        Long id,
        String version,
        String description,
        String author,
        LocalDate releasedAt
) {
}