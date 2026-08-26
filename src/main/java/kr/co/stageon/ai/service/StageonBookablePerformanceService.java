package kr.co.stageon.ai.service;

import kr.co.stageon.ai.dto.AiPerformanceContext;
import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class StageonBookablePerformanceService {
    private static final List<Performance.Status> VISIBLE_STATUSES =
            List.of(Performance.Status.UPCOMING, Performance.Status.ON_SALE);
    private static final List<String> REGIONS = List.of(
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"
    );
    private static final Map<String, String> GENRES = new LinkedHashMap<>();

    static {
        GENRES.put("뮤지컬", "뮤지컬");
        GENRES.put("연극", "연극");
        GENRES.put("콘서트", "콘서트");
        GENRES.put("대중음악", "콘서트");
        GENRES.put("클래식", "클래식/무용");
        GENRES.put("무용", "클래식/무용");
        GENRES.put("발레", "클래식/무용");
        GENRES.put("행사", "행사");
        GENRES.put("전시", "행사");
    }

    private final PerformanceScheduleRepository scheduleRepository;
    private final Clock clock;

    @Autowired
    public StageonBookablePerformanceService(PerformanceScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
        this.clock = Clock.systemDefaultZone();
    }

    public List<AiPerformanceContext> search(String question, int limit) {
        LocalDateTime now = LocalDateTime.now(clock);
        DateRange dateRange = parseDateRange(question, now.toLocalDate());
        String genre = GENRES.entrySet().stream()
                .filter(entry -> question.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        String region = REGIONS.stream().filter(question::contains).findFirst().orElse(null);

        List<PerformanceSchedule> schedules = scheduleRepository.findBookableForAi(
                VISIBLE_STATUSES,
                PerformanceSchedule.Status.CANCELLED,
                ScheduleSeat.Status.AVAILABLE,
                now.toLocalDate(),
                now,
                dateRange.from().atStartOfDay(),
                dateRange.to().plusDays(1).atStartOfDay(),
                genre,
                region,
                PageRequest.of(0, 100)
        );

        Map<Long, List<PerformanceSchedule>> grouped = new LinkedHashMap<>();
        int resultLimit = Math.max(1, Math.min(limit, 20));
        for (PerformanceSchedule schedule : schedules) {
            Long performanceId = schedule.getPerformance().getId();
            if (!grouped.containsKey(performanceId) && grouped.size() >= resultLimit) {
                continue;
            }
            grouped.computeIfAbsent(performanceId, ignored -> new ArrayList<>())
                    .add(schedule);
        }

        return grouped.entrySet().stream()
                .limit(resultLimit)
                .map(entry -> toContext(entry.getValue()))
                .toList();
    }

    private AiPerformanceContext toContext(List<PerformanceSchedule> schedules) {
        PerformanceSchedule firstSchedule = schedules.getFirst();
        Performance performance = firstSchedule.getPerformance();
        var venue = firstSchedule.getVenueHall().getVenue();
        String venueName = venue.getName();
        String hallName = firstSchedule.getVenueHall().getName();
        String venueLabel = venueName.equalsIgnoreCase(hallName)
                ? venueName
                : venueName + " " + hallName;
        String scheduleText = schedules.stream()
                .limit(3)
                .map(schedule -> schedule.getStartsAt().toString().replace('T', ' '))
                .reduce((left, right) -> left + ", " + right)
                .orElse("");

        return new AiPerformanceContext(
                String.valueOf(performance.getId()),
                performance.getTitle(),
                performance.getGenre(),
                performance.getStartDate(),
                performance.getEndDate(),
                venueLabel,
                venue.getRegion(),
                "",
                formatRuntime(performance.getRuntimeMinutes()),
                blank(performance.getAgeText()),
                formatPrice(performance),
                scheduleText,
                "/performances/" + performance.getId(),
                blank(performance.getPosterUrl()),
                blank(performance.getStory()),
                LocalDate.now(clock)
        );
    }

    private DateRange parseDateRange(String question, LocalDate today) {
        if (question.contains("오늘")) {
            return new DateRange(today, today);
        }
        if (question.contains("내일")) {
            LocalDate tomorrow = today.plusDays(1);
            return new DateRange(tomorrow, tomorrow);
        }
        if (question.contains("이번 주말") || question.contains("이번주말")) {
            LocalDate saturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
            return new DateRange(saturday, saturday.plusDays(1));
        }
        if (question.contains("이번 주") || question.contains("이번주")) {
            return new DateRange(today, today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
        }
        return new DateRange(today, today.plusDays(30));
    }

    private String formatRuntime(Integer minutes) {
        if (minutes == null || minutes <= 0) return "";
        int hours = minutes / 60;
        int remainder = minutes % 60;
        if (hours == 0) return remainder + "분";
        if (remainder == 0) return hours + "시간";
        return hours + "시간 " + remainder + "분";
    }

    private String formatPrice(Performance performance) {
        if (performance.getRawPriceText() != null && !performance.getRawPriceText().isBlank()) {
            return performance.getRawPriceText().trim();
        }
        return performance.getBasePrice() == null
                ? ""
                : "기본가 " + NumberFormat.getIntegerInstance(Locale.KOREA)
                        .format(performance.getBasePrice()) + "원";
    }

    private String blank(String value) {
        return value == null ? "" : value.trim();
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }
}
