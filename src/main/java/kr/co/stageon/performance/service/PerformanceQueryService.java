package kr.co.stageon.performance.service;

import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import kr.co.stageon.performance.dto.PerformanceBookingStatusResponse;
import kr.co.stageon.performance.dto.PerformanceDetailResponse;
import kr.co.stageon.performance.dto.PerformanceSummaryResponse;
import kr.co.stageon.performance.dto.ScheduleResponse;
import kr.co.stageon.performance.repository.PerformanceRepository;
import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import kr.co.stageon.venue.repository.SeatGradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceQueryService {

    private static final List<Performance.Status> VISIBLE_STATUSES =
            List.of(Performance.Status.UPCOMING, Performance.Status.ON_SALE);

    private final PerformanceRepository performanceRepository;
    private final PerformanceScheduleRepository scheduleRepository;
    private final SeatGradeRepository seatGradeRepository;
    private final ObjectMapper objectMapper;

    public List<PerformanceSummaryResponse> findPerformances(String keyword, String genre) {
        String normalizedKeyword = normalize(keyword);
        String normalizedGenre = normalize(genre);
        return performanceRepository.findPublished(
                        normalizedKeyword,
                        normalizedGenre,
                        VISIBLE_STATUSES,
                        LocalDate.now(Clock.systemDefaultZone()),
                        PageRequest.of(0, 100)
                )
                .stream().map(PerformanceSummaryResponse::from).toList();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Optional<PerformanceDetailResponse> findPerformance(Long performanceId) {
        return performanceRepository.findPublishedDetailById(
                        performanceId,
                        VISIBLE_STATUSES,
                        LocalDate.now(Clock.systemDefaultZone())
                )
                .map(performance -> PerformanceDetailResponse.from(
                        performance,
                        formatPriceText(performance)
                ));
    }

    public List<ScheduleResponse> findSchedules(Long performanceId) {
        return scheduleRepository.findByPerformanceIdOrderByStartsAtAsc(performanceId)
                .stream().map(ScheduleResponse::from).toList();
    }

    /**
     * 공연 상세 페이지에서 보여줄 현재 예매 상태를 계산합니다.
     *
     * DB의 회차 상태만 보는 것이 아니라,
     * 각 회차의 예매 시작/종료 시간과 현재 시간을 함께 비교합니다.
     */
    public PerformanceBookingStatusResponse findBookingStatus(Long performanceId) {

        // 해당 공연의 전체 회차를 공연 시작 시간 순서대로 조회
        List<PerformanceSchedule> schedules =
                scheduleRepository.findByPerformanceIdOrderByStartsAtAsc(performanceId);

        // 현재 서버 시간을 기준으로 예매 가능 여부 판단
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());

        /*
         * 등록된 회차가 하나도 없는 경우
         *
         * 아직 예매 일정을 등록하지 않은 공연으로 판단합니다.
         */
        if (schedules.isEmpty()) {
            return PerformanceBookingStatusResponse.upcoming(null);
        }

        /*
         * 모든 회차가 CANCELLED 상태인지 확인
         *
         * 하나라도 정상 회차가 있다면 공연 전체를 취소로 보지 않습니다.
         */
        boolean allCancelled = schedules.stream()
                .allMatch(schedule ->
                        schedule.getStatus() == PerformanceSchedule.Status.CANCELLED);

        if (allCancelled) {
            return PerformanceBookingStatusResponse.cancelled();
        }

        /*
         * 취소되지 않은 회차만 사용합니다.
         *
         * 이후 예매 가능/마감/공연 종료 판정에서
         * 취소된 회차가 영향을 주지 않도록 제외합니다.
         */
        List<PerformanceSchedule> activeSchedules = schedules.stream()
                .filter(schedule ->
                        schedule.getStatus() != PerformanceSchedule.Status.CANCELLED)
                .toList();

        /*
         * 모든 정상 회차의 공연 시작 시간이 이미 지났다면
         * 공연 전체가 종료된 것으로 판단합니다.
         */
        boolean allEnded = activeSchedules.stream()
                .allMatch(schedule -> now.isAfter(schedule.getStartsAt()));

        if (allEnded) {
            return PerformanceBookingStatusResponse.ended();
        }

        /*
         * 현재 예매 가능한 회차가 하나라도 있는지 확인
         *
         * salesOpenAt <= 현재시간 < salesCloseAt
         *
         * 시작 시각과 정확히 같은 순간에도 예매 가능하게 하기 위해
         * isAfter()만 사용하지 않고 equals()도 함께 확인합니다.
         */
        boolean available = activeSchedules.stream()
                .anyMatch(schedule ->
                        !now.isBefore(schedule.getSalesOpenAt())
                                && now.isBefore(schedule.getSalesCloseAt()));

        if (available) {
            return PerformanceBookingStatusResponse.available();
        }

        /*
         * 현재 이후에 오픈할 회차 중
         * 가장 가까운 salesOpenAt을 찾습니다.
         */
        LocalDateTime nextOpenAt = activeSchedules.stream()
                .map(PerformanceSchedule::getSalesOpenAt)
                .filter(openAt -> openAt.isAfter(now))
                .min(LocalDateTime::compareTo)
                .orElse(null);

        // 아직 티켓 오픈 전이라면
        // 가장 가까운 오픈 시간을 화면에 전달
        if (nextOpenAt != null) {
            return PerformanceBookingStatusResponse.upcoming(nextOpenAt);
        }

        /*
         * 앞으로 열릴 회차도 없고,
         * 현재 예매 가능한 회차도 없다면
         * 모든 온라인 예매가 마감된 상태입니다.
         */
        return PerformanceBookingStatusResponse.closed();
    }

    private String formatPriceText(Performance performance) {
        String rawPriceText = performance.getRawPriceText();
        if (rawPriceText == null || rawPriceText.isBlank()) {
            return formatBasePrice(performance.getBasePrice());
        }

        try {
            List<SeatPriceValue> prices = objectMapper.readValue(
                    rawPriceText,
                    new TypeReference<List<SeatPriceValue>>() {}
            );
            if (prices == null || prices.isEmpty()) {
                return formatBasePrice(performance.getBasePrice());
            }

            List<Long> gradeIds = prices.stream()
                    .map(SeatPriceValue::gradeId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            Map<Long, String> gradeNames = new LinkedHashMap<>();
            seatGradeRepository.findAllById(gradeIds)
                    .forEach(grade -> gradeNames.put(grade.getId(), grade.getName()));

            String formatted = prices.stream()
                    .filter(price -> price.price() != null)
                    .map(price -> {
                        String gradeName = gradeNames.getOrDefault(price.gradeId(), "좌석");
                        return gradeName + " " + formatWon(price.price());
                    })
                    .collect(Collectors.joining(" · "));
            return formatted.isBlank() ? formatBasePrice(performance.getBasePrice()) : formatted;
        } catch (JacksonException ignored) {
            return rawPriceText.trim();
        }
    }

    private static String formatBasePrice(Integer basePrice) {
        return basePrice == null ? null : "기본가 " + formatWon(basePrice);
    }

    private static String formatWon(Integer price) {
        return NumberFormat.getIntegerInstance(Locale.KOREA).format(price) + "원";
    }

    private record SeatPriceValue(Long gradeId, Integer price) {
    }

}
