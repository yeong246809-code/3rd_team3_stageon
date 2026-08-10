package kr.co.stageon.performance.service;

import kr.co.stageon.performance.domain.Performance;
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
