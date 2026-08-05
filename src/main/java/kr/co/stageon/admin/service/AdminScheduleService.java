package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.HallOptionDto;
import kr.co.stageon.admin.dto.ScheduleFormDto;
import kr.co.stageon.admin.dto.ScheduleListItemDto;
import kr.co.stageon.admin.dto.ScheduleOverviewItemDto;
import kr.co.stageon.admin.dto.ScheduleStatsDto;
import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import kr.co.stageon.performance.repository.PerformanceRepository;
import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import kr.co.stageon.venue.domain.SeatChart;
import kr.co.stageon.venue.domain.VenueHall;
import kr.co.stageon.venue.repository.SeatChartRepository;
import kr.co.stageon.venue.repository.VenueHallRepository;
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

/** AD07 "일정·회차 관리" 화면의 통계/요약/상세 목록/회차 생성·상태 변경을 담당합니다. */
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
    private final PerformanceRepository performanceRepository;
    private final VenueHallRepository venueHallRepository;
    private final SeatChartRepository seatChartRepository;

    @Transactional(readOnly = true)
    public ScheduleStatsDto getStats() {
        long operating = scheduleRepository.countByStatus(PerformanceSchedule.Status.OPEN);
        long upcoming = scheduleRepository.countByStatus(PerformanceSchedule.Status.SCHEDULED);
        List<PerformanceSchedule> active = scheduleRepository.findActiveWithDetails(ACTIVE_STATUSES);
        long conflict = computeConflictingIds(active).size();
        return new ScheduleStatsDto(operating, upcoming, conflict);
    }

    @Transactional(readOnly = true)
    public List<ScheduleOverviewItemDto> getOverview() {
        return getOverview(false, false);
    }

    /** all=false면 최근 5건, all=true면 활성 회차 전체를 반환합니다("전체 보기" 토글용). conflictOnly=true면 충돌 회차만 반환합니다. */
    @Transactional(readOnly = true)
    public List<ScheduleOverviewItemDto> getOverview(boolean all, boolean conflictOnly) {
        List<PerformanceSchedule> active = scheduleRepository.findActiveWithDetails(ACTIVE_STATUSES);
        Set<Long> conflictIds = computeConflictingIds(active);

        var filtered = conflictOnly
                ? active.stream().filter(s -> conflictIds.contains(s.getId()))
                : active.stream();
        if (!conflictOnly && !all) {
            filtered = filtered.limit(5);
        }
        return filtered
                .map(s -> new ScheduleOverviewItemDto(
                        s.getId(),
                        s.getPerformance().getTitle(),
                        s.getStartsAt().format(DATE_TIME_FMT),
                        statusText(s.getStatus()),
                        badgeClass(s.getStatus()),
                        conflictIds.contains(s.getId())
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleListItemDto> getListByPerformanceAndMonth(Long performanceId, YearMonth month) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        List<PerformanceSchedule> schedules = scheduleRepository.findByPerformanceAndMonth(performanceId, from, to);

        List<PerformanceSchedule> active = scheduleRepository.findActiveWithDetails(ACTIVE_STATUSES);
        Set<Long> conflictIds = computeConflictingIds(active);

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
                    remaining,
                    conflictIds.contains(s.getId()),
                    s.getStatus().name()
            ));
        }
        return result;
    }

    /** "회차 추가" 모달의 공연장·홀 드롭다운 옵션입니다. 활성 홀만 노출합니다. */
    @Transactional(readOnly = true)
    public List<HallOptionDto> getHallOptions() {
        return venueHallRepository.findAll().stream()
                .filter(VenueHall::isActive)
                .sorted(Comparator.comparing(h -> h.getVenue().getName() + h.getName()))
                .map(h -> new HallOptionDto(h.getId(), h.getVenue().getName() + " · " + h.getName()))
                .collect(Collectors.toList());
    }

    /**
     * 신규 회차를 생성합니다. 선택한 홀의 활성 좌석도를 그대로 사용하며,
     * 홀에 좌석도가 없으면 예외를 던집니다(공연장·좌석 관리에서 먼저 등급 등록 필요).
     * 판매 시작·종료 시각을 입력하지 않으면 회차 시작 시각을 기준으로 자동 설정합니다.
     */
    @Transactional
    public Long createSchedule(ScheduleFormDto dto) {
        if (dto.getPerformanceId() == null || dto.getVenueHallId() == null || dto.getStartsAt() == null) {
            throw new IllegalArgumentException("공연·공연장·시작 시간은 필수입니다.");
        }

        Performance performance = performanceRepository.findById(dto.getPerformanceId())
                .orElseThrow(() -> new IllegalArgumentException("공연을 찾을 수 없습니다. id=" + dto.getPerformanceId()));
        VenueHall hall = venueHallRepository.findById(dto.getVenueHallId())
                .orElseThrow(() -> new IllegalArgumentException("공연장 홀을 찾을 수 없습니다. id=" + dto.getVenueHallId()));
        SeatChart chart = seatChartRepository.findFirstByVenueHallIdAndActiveTrueOrderByVersionDesc(hall.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "'" + hall.getName() + "' 홀에 등록된 좌석도가 없습니다. 공연장·좌석 관리에서 좌석 등급을 먼저 등록해주세요."));

        LocalDateTime startsAt = dto.getStartsAt();
        LocalDateTime salesOpenAt = dto.getSalesOpenAt() != null ? dto.getSalesOpenAt() : startsAt.minusDays(14);
        LocalDateTime salesCloseAt = dto.getSalesCloseAt() != null ? dto.getSalesCloseAt() : startsAt;
        Integer maxTickets = dto.getMaxTicketsPerMember() != null ? dto.getMaxTicketsPerMember() : 4;

        PerformanceSchedule schedule = PerformanceSchedule.create(
                performance, hall, chart, dto.getRoundNumber(), startsAt,
                salesOpenAt, salesCloseAt, dto.getCancelCloseAt(), maxTickets,
                PerformanceSchedule.Status.SCHEDULED
        );
        return scheduleRepository.save(schedule).getId();
    }

    /** 회차 판매 상태를 변경합니다(판매 시작/판매 종료/취소). */
    @Transactional
    public void changeStatus(Long scheduleId, PerformanceSchedule.Status newStatus) {
        PerformanceSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다. id=" + scheduleId));
        schedule.changeStatus(newStatus);
    }

    /**
     * 같은 공연장 홀에서 시간대가 겹치는 회차 id 집합을 계산합니다.
     * 공연 러닝타임을 종료 시각 추정에 사용하며, 정렬 후 인접한 회차끼리만 비교하는 근사치입니다.
     */
    private Set<Long> computeConflictingIds(List<PerformanceSchedule> active) {
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
        return conflicting;
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