package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.ScheduleListItemDto;
import kr.co.stageon.admin.dto.ScheduleOverviewItemDto;
import kr.co.stageon.admin.dto.ScheduleStatsDto;
import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** AD07 "일정·회차 관리" 화면의 통계/요약/상세 목록을 조합합니다. */
@Service
@RequiredArgsConstructor
public class AdminScheduleService {

    /** 공연 러닝타임이 비어 있을 때 충돌 검사에 사용하는 기본 소요 시간(분)입니다. */
    private static final long DEFAULT_RUNTIME_MINUTES = 150;
    private static final List<PerformanceSchedule.Status> ACTIVE_STATUSES =
            List.of(PerformanceSchedule.Status.SCHEDULED, PerformanceSchedule.Status.OPEN);
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("MM.dd HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final PerformanceScheduleRepository scheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;

    @Transactional(readOnly = true)
    public ScheduleStatsDto getStats() {
        long operating = scheduleRepository.countByStatus(PerformanceSchedule.Status.OPEN);
        long upcoming = scheduleRepository.countByStatus(PerformanceSchedule.Status.SCHEDULED);
        List<PerformanceSchedule> active = scheduleRepository.findActiveWithDetails(ACTIVE_STATUSES);
        long conflict = computeConflictCount(active);
        return new ScheduleStatsDto(operating, upcoming, conflict);
    }

    @Transactional(readOnly = true)
    public List<ScheduleOverviewItemDto> getOverview() {
        return scheduleRepository.findActiveWithDetails(ACTIVE_STATUSES).stream()
                .limit(5)
                .map(s -> new ScheduleOverviewItemDto(
                        s.getId(),
                        s.getPerformance().getTitle(),
                        s.getStartsAt().format(DATE_TIME_FMT),
                        statusText(s.getStatus()),
                        badgeClass(s.getStatus())
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleListItemDto> getListByPerformanceAndMonth(Long performanceId, YearMonth month) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        List<PerformanceSchedule> schedules = scheduleRepository.findByPerformanceAndMonth(performanceId, from, to);

        List<ScheduleListItemDto> result = new ArrayList<>();
        for (PerformanceSchedule s : schedules) {
            long remaining = scheduleSeatRepository.countByScheduleIdAndStatus(s.getId(), ScheduleSeat.Status.AVAILABLE);
            String round = s.getRoundNumber() != null ? s.getRoundNumber() + "회" : "-";
            result.add(new ScheduleListItemDto(
                    s.getId(),
                    s.getStartsAt().toLocalDate().format(DATE_FMT),
                    round,
                    s.getStartsAt().toLocalTime().format(TIME_FMT),
                    statusText(s.getStatus()),
                    badgeClass(s.getStatus()),
                    remaining
            ));
        }
        return result;
    }

    /**
     * 같은 공연장 홀에서 시간대가 겹치는 회차 수를 계산합니다.
     * 공연 러닝타임을 종료 시각 추정에 사용하며, 정렬 후 인접한 회차끼리만 비교하는 근사치입니다.
     */
    private long computeConflictCount(List<PerformanceSchedule> active) {
        Map<Long, List<PerformanceSchedule>> byHall = active.stream()
                .collect(Collectors.groupingBy(s -> s.getVenueHall().getId()));

        Set<Long> conflicting = new HashSet<>();
        for (List<PerformanceSchedule> hallSchedules : byHall.values()) {
            List<PerformanceSchedule> sorted = hallSchedules.stream()
                    .sorted(Comparator.comparing(PerformanceSchedule::getStartsAt))
                    .collect(Collectors.toList());
            for (int i = 0; i < sorted.size() - 1; i++) {
                PerformanceSchedule current = sorted.get(i);
                PerformanceSchedule next = sorted.get(i + 1);
                LocalDateTime currentEnd = current.getStartsAt().plusMinutes(runtimeOf(current));
                if (next.getStartsAt().isBefore(currentEnd)) {
                    conflicting.add(current.getId());
                    conflicting.add(next.getId());
                }
            }
        }
        return conflicting.size();
    }

    private long runtimeOf(PerformanceSchedule schedule) {
        Integer minutes = schedule.getPerformance().getRuntimeMinutes();
        return (minutes != null && minutes > 0) ? minutes : DEFAULT_RUNTIME_MINUTES;
    }

    private String statusText(PerformanceSchedule.Status status) {
        return switch (status) {
            case OPEN -> "판매 중";
            case SCHEDULED -> "판매 예정";
            case CLOSED -> "판매 종료";
            case CANCELLED -> "취소";
        };
    }

    private String badgeClass(PerformanceSchedule.Status status) {
        return switch (status) {
            case OPEN -> "badge--on-sale";
            case SCHEDULED -> "badge--upcoming";
            case CLOSED -> "badge--ended";
            case CANCELLED -> "badge--cancelled";
        };
    }
}