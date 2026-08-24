package kr.co.stageon.ai.dto;

import java.time.LocalDate;

public record AiPerformanceContext(
        String id,
        String name,
        String genre,
        LocalDate startDate,
        LocalDate endDate,
        String venue,
        String region,
        String cast,
        String runtime,
        String ageRating,
        String price,
        String schedule,
        String bookingUrl,
        String posterUrl,
        String description,
        LocalDate updatedAt
) {
}
