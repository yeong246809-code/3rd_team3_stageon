package kr.co.stageon.home.dto;

import kr.co.stageon.performance.domain.PerformanceSchedule;
import kr.co.stageon.performance.support.PerformanceGenres;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** 홈의 티켓 오픈 예정 카드 한 건입니다. */
public record HomeTicketOpenView(
        Long performanceId,
        String title,
        String genre,
        String imageUrl,
        String fallbackImageUrl,
        String venueName,
        String badgeText,
        String salesOpenText
) {
    private static final DateTimeFormatter OPEN_FORMATTER =
            DateTimeFormatter.ofPattern("MM.dd (E) HH:mm", Locale.KOREAN);

    public static HomeTicketOpenView from(PerformanceSchedule schedule, LocalDateTime now) {
        var performance = schedule.getPerformance();
        var hall = schedule.getVenueHall();
        String venueName = hall.getVenue().getName() + " " + hall.getName();
        long days = ChronoUnit.DAYS.between(now.toLocalDate(), schedule.getSalesOpenAt().toLocalDate());

        return new HomeTicketOpenView(
                performance.getId(),
                performance.getTitle(),
                performance.getGenre(),
                performance.getPosterUrl(),
                PerformanceGenres.defaultPosterFor(performance.getGenre()),
                venueName,
                days == 0 ? "TODAY" : "D-" + days,
                OPEN_FORMATTER.format(schedule.getSalesOpenAt()) + " 티켓 오픈"
        );
    }
}
