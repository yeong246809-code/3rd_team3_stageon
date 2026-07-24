package kr.co.stageon.performance.dto;

import kr.co.stageon.performance.domain.Performance;

import java.time.LocalDate;

/** 공연 상세 화면에서 사용하는 DTO입니다. */
public record PerformanceDetailResponse(
        Long id,
        String kopisId,
        String title,
        String genre,
        String posterUrl,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String sourceType
) {
    public static PerformanceDetailResponse from(Performance performance) {
        return new PerformanceDetailResponse(
                performance.getId(),
                performance.getKopisId(),
                performance.getTitle(),
                performance.getGenre(),
                performance.getPosterUrl(),
                performance.getStartDate(),
                performance.getEndDate(),
                performance.getStatus().name(),
                performance.getSourceType().name()
        );
    }
}
