package kr.co.stageon.performance.dto;

import kr.co.stageon.performance.domain.Performance;

import java.time.LocalDate;

/** 목록 화면과 검색 API에서 사용하는 공연 요약 DTO입니다. */
public record PerformanceSummaryResponse(
        Long id,
        String title,
        String genre,
        String posterUrl,
        LocalDate startDate,
        LocalDate endDate,
        String status
) {
    public static PerformanceSummaryResponse from(Performance performance) {
        return new PerformanceSummaryResponse(
                performance.getId(),
                performance.getTitle(),
                performance.getGenre(),
                performance.getPosterUrl(),
                performance.getStartDate(),
                performance.getEndDate(),
                performance.getStatus().name()
        );
    }
}
