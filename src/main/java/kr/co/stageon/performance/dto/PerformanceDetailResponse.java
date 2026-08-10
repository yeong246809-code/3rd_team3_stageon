package kr.co.stageon.performance.dto;

import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.support.PerformanceGenres;

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
        Integer basePrice,
        String status,
        String statusLabel,
        String genreLabel,
        String fallbackPosterUrl,
        String runtimeText,
        String priceText,
        String venueName,
        String venueHallName,
        String region,
        String venueAddress,
        LocalDateTime kopisUpdatedAt,
        LocalDateTime lastSyncedAt
) {
    public static PerformanceDetailResponse from(Performance performance, String priceText) {
        var venueHall = performance.getVenueHall();
        var venue = venueHall == null ? null : venueHall.getVenue();

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
                performance.getBasePrice(),
                performance.getStatus().name(),
                statusLabel(performance.getStatus()),
                PerformanceGenres.labelFor(performance.getGenre()),
                PerformanceGenres.defaultPosterFor(performance.getGenre()),
                formatRuntime(performance.getRuntimeMinutes()),
                priceText,
                venue == null ? null : venue.getName(),
                venueHall == null ? null : venueHall.getName(),
                venue == null ? null : venue.getRegion(),
                venue == null ? null : venue.getAddress(),
                performance.getKopisUpdatedAt(),
                performance.getLastSyncedAt()
        );
    }

    private static String formatRuntime(Integer runtimeMinutes) {
        if (runtimeMinutes == null || runtimeMinutes <= 0) {
            return null;
        }

        int hours = runtimeMinutes / 60;
        int minutes = runtimeMinutes % 60;
        if (hours == 0) {
            return minutes + "분";
        }
        if (minutes == 0) {
            return hours + "시간";
        }
        return hours + "시간 " + minutes + "분";
    }

    private static String statusLabel(Performance.Status status) {
        return switch (status) {
            case ON_SALE -> "예매 가능";
            case UPCOMING -> "오픈 예정";
            case ENDED -> "공연 종료";
            case CANCELLED -> "공연 취소";
        };
    }
}
