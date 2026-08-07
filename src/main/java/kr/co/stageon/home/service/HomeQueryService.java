package kr.co.stageon.home.service;

import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.home.dto.HomePageView;
import kr.co.stageon.home.dto.HomeGenreRankingView;
import kr.co.stageon.home.dto.HomePerformanceView;
import kr.co.stageon.home.dto.HomeRankingView;
import kr.co.stageon.home.dto.HomeTicketOpenView;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import kr.co.stageon.performance.repository.PerformanceRepository;
import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import kr.co.stageon.performance.support.PerformanceGenres;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 홈 화면에 필요한 공개 공연 데이터를 읽기 전용으로 조합합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeQueryService {

    private static final List<Performance.Status> VISIBLE_PERFORMANCE_STATUSES =
            List.of(Performance.Status.UPCOMING, Performance.Status.ON_SALE);
    private static final List<PerformanceSchedule.Status> VISIBLE_SCHEDULE_STATUSES =
            List.of(PerformanceSchedule.Status.SCHEDULED, PerformanceSchedule.Status.OPEN);

    private final PerformanceRepository performanceRepository;
    private final PerformanceScheduleRepository scheduleRepository;
    private final ReservationRepository reservationRepository;

    @Value("${stageon.home.featured-performance-id:0}")
    private long featuredPerformanceId;

    public HomePageView getHomePage() {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());

        List<Performance> performances = performanceRepository.findPublishedForHome(
                VISIBLE_PERFORMANCE_STATUSES,
                now.toLocalDate(),
                PageRequest.of(0, 12)
        );

        List<HomePerformanceView> banners = performances.stream()
                .limit(3)
                .map(HomePerformanceView::from)
                .toList();

        List<HomeTicketOpenView> ticketOpenings = scheduleRepository.findUpcomingTicketOpenings(
                        now,
                        VISIBLE_SCHEDULE_STATUSES,
                        VISIBLE_PERFORMANCE_STATUSES,
                        PageRequest.of(0, 20)
                ).stream()
                .filter(HomeQueryService.distinctPerformance())
                .limit(4)
                .map(schedule -> HomeTicketOpenView.from(schedule, now))
                .toList();

        LocalDateTime rankingFrom = now.minusDays(7);
        var rankingRows = reservationRepository.findPerformanceRankings(
                Reservation.Status.RESERVED,
                rankingFrom,
                VISIBLE_PERFORMANCE_STATUSES,
                now.toLocalDate()
        );
        List<HomeGenreRankingView> genreRankings = PerformanceGenres.options().stream()
                .map(option -> toGenreRanking(option, rankingRows))
                .toList();

        HomePerformanceView featured = featuredPerformanceId > 0
                ? performanceRepository.findPublishedByIdForHome(
                                featuredPerformanceId,
                                VISIBLE_PERFORMANCE_STATUSES,
                                now.toLocalDate()
                        )
                        .map(HomePerformanceView::from)
                        .orElse(null)
                : null;

        return new HomePageView(banners, ticketOpenings, genreRankings, featured);
    }

    private static HomeGenreRankingView toGenreRanking(
            PerformanceGenres.Option option,
            List<ReservationRepository.PerformanceRankingProjection> rows
    ) {
        var genreRows = rows.stream()
                .filter(row -> option.value().equals(row.getGenre()))
                .toList();
        long totalTickets = genreRows.stream()
                .mapToLong(ReservationRepository.PerformanceRankingProjection::getTicketCount)
                .sum();
        var rankings = genreRows.stream()
                .limit(4)
                .map(row -> HomeRankingView.from(row, totalTickets))
                .toList();
        return new HomeGenreRankingView(option.value(), option.label(), rankings);
    }

    private static java.util.function.Predicate<PerformanceSchedule> distinctPerformance() {
        var seenIds = new java.util.HashSet<Long>();
        return schedule -> seenIds.add(schedule.getPerformance().getId());
    }
}
