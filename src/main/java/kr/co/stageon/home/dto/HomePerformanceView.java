package kr.co.stageon.home.dto;

import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.support.PerformanceGenres;

import java.time.format.DateTimeFormatter;

/** 홈 배너와 특집 공연 영역에서 공통으로 사용하는 읽기 전용 모델입니다. */
public record HomePerformanceView(
        Long id,
        String title,
        String genre,
        String imageUrl,
        String fallbackImageUrl,
        String periodText,
        String description,
        String statusLabel
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static HomePerformanceView from(Performance performance) {
        return new HomePerformanceView(
                performance.getId(),
                performance.getTitle(),
                performance.getGenre(),
                performance.getPosterUrl(),
                PerformanceGenres.defaultPosterFor(performance.getGenre()),
                DATE_FORMATTER.format(performance.getStartDate())
                        + " ~ " + DATE_FORMATTER.format(performance.getEndDate()),
                summarize(performance),
                statusLabel(performance.getStatus())
        );
    }

    private static String summarize(Performance performance) {
        String story = performance.getStory();
        if (story == null || story.isBlank()) {
            return performance.getGenre() + " 공연의 새로운 무대를 StageOn에서 만나보세요.";
        }

        String normalized = story.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 140 ? normalized : normalized.substring(0, 140) + "…";
    }

    private static String statusLabel(Performance.Status status) {
        return switch (status) {
            case UPCOMING -> "COMING SOON";
            case ON_SALE -> "NOW ON SALE";
            case ENDED -> "CLOSED";
            case CANCELLED -> "CANCELLED";
        };
    }
}
