package kr.co.stageon.performance.dto;

import kr.co.stageon.performance.domain.Performance;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 공연 상세 화면에서 사용하는 DTO입니다. */
public record PerformanceDetailResponse(
        Long id,
        String kopisId,
        String title,
        String genre,
        String posterUrl,
        LocalDate startDate,
        LocalDate endDate,
        Integer runtimeMinutes,
        String ageText,
        String story,
        String rawPriceText,
        String rawScheduleText,
        String status,
        LocalDateTime kopisUpdatedAt,
        LocalDateTime lastSyncedAt
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
                performance.getRuntimeMinutes(),
                performance.getAgeText(),
                performance.getStory(),
                performance.getRawPriceText(),
                performance.getRawScheduleText(),
                performance.getStatus().name(),
                performance.getKopisUpdatedAt(),
                performance.getLastSyncedAt()
        );
    }
}
