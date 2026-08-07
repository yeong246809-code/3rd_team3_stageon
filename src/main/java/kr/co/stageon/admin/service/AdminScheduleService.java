package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.BulkCreateResultDto;
import kr.co.stageon.admin.dto.HallOptionDto;
import kr.co.stageon.admin.dto.ScheduleBulkFormDto;
import kr.co.stageon.admin.dto.ScheduleEditFormDto;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** AD07 "일정·회차 관리" 화면의 통계/요약/상세 목록/회차 생성·수정·삭제·상태 변경을 담당합니다. */
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
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

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
                        conflictIds.contains(s.getId()),
                        s.getPerformance().getId(),
                        s.getVenueHall().getId(),
                        s.getRoundNumber(),
                        s.getMaxTicketsPerMember(),
                        s.getStartsAt().format(ISO_FMT),
                        s.getSalesOpenAt() != null ? s.getSalesOpenAt().format(ISO_FMT) : null,
                        s.getSalesCloseAt() != null ? s.getSalesCloseAt().format(ISO_FMT) : null,
                        s.getCancelCloseAt() != null ? s.getCancelCloseAt().format(ISO_FMT) : null
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
                    s.getStatus().name(),
                    s.getPerformance().getId(),
                    s.getVenueHall().getId(),
                    s.getRoundNumber(),
                    s.getMaxTicketsPerMember(),
                    s.getStartsAt().format(ISO_FMT),
                    s.getSalesOpenAt() != null ? s.getSalesOpenAt().format(ISO_FMT) : null,
                    s.getSalesCloseAt() != null ? s.getSalesCloseAt().format(ISO_FMT) : null,
                    s.getCancelCloseAt() != null ? s.getCancelCloseAt().format(ISO_FMT) : null
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
     * 특정 홀에서 다른 공연이 이미 점유한 날짜 목록입니다(yyyy-MM-dd 문자열).
     * 회차 추가 달력에서 해당 날짜를 클릭 못 하도록 막는 데 사용합니다.
     */
    @Transactional(readOnly = true)
    public Set<String> getHallOccupiedDates(Long hallId, Long excludePerformanceId) {
        if (hallId == null || excludePerformanceId == null) {
            return Set.of();
        }
        return scheduleRepository.findOtherPerformanceSchedulesInHall(hallId, excludePerformanceId).stream()
                .map(s -> s.getStartsAt().toLocalDate().toString())
                .collect(Collectors.toSet());
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

        LocalDate targetDate = dto.getStartsAt().toLocalDate();
        checkHallDateConflict(hall.getId(), performance.getId(), targetDate);

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

    /**
     * 기존 회차의 회차 번호·일정·매수를 수정합니다. 공연·공연장 홀·좌석도는 변경하지 않습니다.
     * 판매 시작·종료를 비워두면 각각 공연 시작 14일 전 / 공연 시작 시각으로 자동 설정됩니다.
     */
    @Transactional
    public void updateSchedule(Long scheduleId, ScheduleEditFormDto dto) {
        if (dto.getStartsAt() == null) {
            throw new IllegalArgumentException("공연 시작 시간은 필수입니다.");
        }
        PerformanceSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다. id=" + scheduleId));

        LocalDateTime startsAt = dto.getStartsAt();
        LocalDateTime salesOpenAt = dto.getSalesOpenAt() != null ? dto.getSalesOpenAt() : startsAt.minusDays(14);
        LocalDateTime salesCloseAt = dto.getSalesCloseAt() != null ? dto.getSalesCloseAt() : startsAt;
        Integer maxTickets = dto.getMaxTicketsPerMember() != null ? dto.getMaxTicketsPerMember() : 4;

        schedule.updateTiming(dto.getRoundNumber(), startsAt, salesOpenAt, salesCloseAt, dto.getCancelCloseAt(), maxTickets);
    }

    /**
     * 회차를 삭제합니다. 판매중(OPEN)·판매종료(CLOSED) 상태의 회차는 삭제할 수 없으며,
     * 먼저 "회차 취소"로 상태를 변경한 뒤 삭제해야 합니다(이미 예매/판매가 있었을 수 있으므로 보호).
     * 예매·결제 등 연결된 데이터가 있어 DB 제약으로 삭제가 막히는 경우에도 안내 메시지를 던집니다.
     */
    @Transactional
    public void deleteSchedule(Long scheduleId) {
        PerformanceSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다. id=" + scheduleId));

        if (schedule.getStatus() == PerformanceSchedule.Status.OPEN
                || schedule.getStatus() == PerformanceSchedule.Status.CLOSED) {
            throw new IllegalStateException("판매 중이거나 판매 종료된 회차는 삭제할 수 없습니다. 먼저 '회차 취소'로 상태를 변경해주세요.");
        }

        try {
            scheduleRepository.delete(schedule);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("이미 예매·결제 등 연결된 데이터가 있어 이 회차는 삭제할 수 없습니다.");
        }
    }

    /**
     * 달력에서 선택한 여러 날짜에 같은 시간으로 회차를 한 번에 생성합니다.
     * 홀에 좌석도가 없으면 예외를 던지며, 개별 날짜 파싱에 실패한 항목은 건너뛰고 개수로 집계합니다.
     */
    @Transactional
    public BulkCreateResultDto createBulkSchedules(ScheduleBulkFormDto dto) {
        if (dto.getPerformanceId() == null || dto.getVenueHallId() == null
                || dto.getDatesCsv() == null || dto.getDatesCsv().isBlank() || dto.getTime() == null || dto.getTime().isBlank()) {
            throw new IllegalArgumentException("공연·공연장·날짜·시간은 모두 필수입니다.");
        }

        Performance performance = performanceRepository.findById(dto.getPerformanceId())
                .orElseThrow(() -> new IllegalArgumentException("공연을 찾을 수 없습니다. id=" + dto.getPerformanceId()));
        VenueHall hall = venueHallRepository.findById(dto.getVenueHallId())
                .orElseThrow(() -> new IllegalArgumentException("공연장 홀을 찾을 수 없습니다. id=" + dto.getVenueHallId()));
        SeatChart chart = seatChartRepository.findFirstByVenueHallIdAndActiveTrueOrderByVersionDesc(hall.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "'" + hall.getName() + "' 홀에 등록된 좌석도가 없습니다. 공연장·좌석 관리에서 좌석 등급을 먼저 등록해주세요."));

        LocalTime time = LocalTime.parse(dto.getTime());
        Integer maxTickets = dto.getMaxTicketsPerMember() != null ? dto.getMaxTicketsPerMember() : 4;

        int created = 0;
        int skipped = 0;
        for (String raw : dto.getDatesCsv().split(",")) {
            String value = raw.trim();
            if (value.isEmpty()) continue;
            try {
                LocalDate date = LocalDate.parse(value);
                checkHallDateConflict(hall.getId(), performance.getId(), date);

                LocalDateTime startsAt = LocalDateTime.of(date, time);
                LocalDateTime salesOpenAt = startsAt.minusDays(14);
                LocalDateTime salesCloseAt = startsAt;

                PerformanceSchedule schedule = PerformanceSchedule.create(
                        performance, hall, chart, null, startsAt,
                        salesOpenAt, salesCloseAt, null, maxTickets,
                        PerformanceSchedule.Status.SCHEDULED
                );
                scheduleRepository.save(schedule);
                created++;
            } catch (Exception e) {
                skipped++;
            }
        }
        return new BulkCreateResultDto(created, skipped);
    }

    /**
     * 같은 홀, 같은 날짜에 다른 공연의 회차가 이미 있으면 예외를 던집니다.
     * 같은 Performance ID(같은 공연의 다른 회차)는 예외로 허용합니다.
     */
    private void checkHallDateConflict(Long hallId, Long performanceId, LocalDate date) {
        boolean conflict = scheduleRepository.findOtherPerformanceSchedulesInHall(hallId, performanceId).stream()
                .anyMatch(s -> s.getStartsAt().toLocalDate().equals(date));
        if (conflict) {
            throw new IllegalStateException(
                    date + " 날짜에는 해당 공연장에 이미 다른 공연이 등록되어 있습니다.");
        }
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