package kr.co.stageon.performance.service;

import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.dto.PerformanceDetailResponse;
import kr.co.stageon.performance.dto.PerformanceSummaryResponse;
import kr.co.stageon.performance.dto.ScheduleResponse;
import kr.co.stageon.performance.repository.PerformanceRepository;
import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceQueryService {

    private static final List<Performance.Status> VISIBLE_STATUSES =
            List.of(Performance.Status.UPCOMING, Performance.Status.ON_SALE);

    private final PerformanceRepository performanceRepository;
    private final PerformanceScheduleRepository scheduleRepository;

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
        return performanceRepository.findById(performanceId).map(PerformanceDetailResponse::from);
    }

    public List<ScheduleResponse> findSchedules(Long performanceId) {
        return scheduleRepository.findByPerformanceIdOrderByStartsAtAsc(performanceId)
                .stream().map(ScheduleResponse::from).toList();
    }

}
