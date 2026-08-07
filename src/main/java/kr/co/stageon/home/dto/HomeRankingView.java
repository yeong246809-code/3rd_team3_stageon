package kr.co.stageon.home.dto;

import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.performance.support.PerformanceGenres;

import java.util.Locale;

/** 최근 예매 건수를 기준으로 계산한 홈 랭킹 카드입니다. */
public record HomeRankingView(
        Long performanceId,
        String title,
        String genre,
        String imageUrl,
        String fallbackImageUrl,
        long ticketCount,
        String shareText
) {
    public static HomeRankingView from(
            ReservationRepository.PerformanceRankingProjection row,
            long totalReservations
    ) {
        double share = totalReservations == 0
                ? 0
                : (row.getTicketCount() * 100.0) / totalReservations;

        return new HomeRankingView(
                row.getPerformanceId(),
                row.getTitle(),
                row.getGenre(),
                row.getPosterUrl(),
                PerformanceGenres.defaultPosterFor(row.getGenre()),
                row.getTicketCount(),
                String.format(Locale.KOREAN, "%.1f%%", share)
        );
    }
}
