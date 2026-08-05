package kr.co.stageon.performance.service;

import kr.co.stageon.performance.dto.PerformanceDetailResponse;
import kr.co.stageon.performance.dto.PerformanceSummaryResponse;
import kr.co.stageon.performance.dto.ScheduleResponse;
import kr.co.stageon.performance.repository.PerformanceRepository;
import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceQueryService {

    private final PerformanceRepository performanceRepository;
    private final PerformanceScheduleRepository scheduleRepository;

    public List<PerformanceSummaryResponse> findPerformances(String keyword, String genre) {
        if (keyword != null && !keyword.isBlank()) {
            return performanceRepository.findByTitleContainingIgnoreCaseOrderByStartDateAsc(keyword.trim())
                    .stream().map(PerformanceSummaryResponse::from).toList();
        }
        if (genre != null && !genre.isBlank()) {
            return performanceRepository.findByGenreOrderByStartDateAsc(genre.trim())
                    .stream().map(PerformanceSummaryResponse::from).toList();
        }
        return performanceRepository.findTop12ByOrderByStartDateAsc()
                .stream().map(PerformanceSummaryResponse::from).toList();
    }

    public Optional<PerformanceDetailResponse> findPerformance(Long performanceId) {
        return performanceRepository.findById(performanceId).map(PerformanceDetailResponse::from);
    }

    public List<ScheduleResponse> findSchedules(Long performanceId) {
        return scheduleRepository.findByPerformanceIdOrderByStartsAtAsc(performanceId)
                .stream().map(ScheduleResponse::from).toList();
    }

}
