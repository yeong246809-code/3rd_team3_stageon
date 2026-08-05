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

// ===== [남수아 담당: 날짜·회차 선택 화면 추가 시작] =====

import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.performance.dto.ScheduleSelectionResponse;
import kr.co.stageon.performance.dto.SeatGradeAvailabilityResponse;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

// ===== [남수아 담당: 날짜·회차 선택 화면 추가 끝] =====

/** 공연 목록·검색·상세·회차 조회의 트랜잭션 경계를 담당합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceQueryService {

    private final PerformanceRepository performanceRepository;
    private final PerformanceScheduleRepository scheduleRepository;

    // ===== [남수아 담당: 회차별 좌석 재고 조회] =====

    /**
     * 회차별 좌석 상태와 좌석 등급 정보를 조회합니다.
     *
     * schedule_seats
     * → seats
     * → seat_grades
     * 관계를 사용합니다.
     */
    private final ScheduleSeatRepository scheduleSeatRepository;

    // ===== [남수아 담당: 회차별 좌석 재고 조회 끝] =====

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
