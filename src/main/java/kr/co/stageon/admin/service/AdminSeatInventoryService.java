package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.SeatInventoryDeleteResultDto;
import kr.co.stageon.admin.dto.SeatInventoryGradeDto;
import kr.co.stageon.admin.dto.SeatInventoryGradeOptionDto;
import kr.co.stageon.admin.dto.SeatInventoryHoldDto;
import kr.co.stageon.admin.dto.SeatInventoryRowDto;
import kr.co.stageon.admin.dto.SeatInventorySeatDto;
import kr.co.stageon.admin.dto.SeatInventorySectionDto;
import kr.co.stageon.admin.dto.SeatInventoryScheduleOptionDto;
import kr.co.stageon.admin.dto.SeatInventoryStatsDto;
import kr.co.stageon.admin.dto.SeatInventoryUnassignedSeatDto;
import kr.co.stageon.admin.dto.SeatInventoryUnassignedSectionDto;
import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.booking.repository.SeatHoldItemRepository;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import kr.co.stageon.venue.domain.Seat;
import kr.co.stageon.venue.domain.SeatGrade;
import kr.co.stageon.venue.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AD08 "좌석 재고·선점 현황" 화면의 통계/등급별 잔여석/좌석 배치도/활성 선점 조회와
 * 회차 좌석 구성(개별·일괄 생성, 신규 물리 좌석 생성, 개별·일괄 삭제), 관리자 좌석 상태 일괄 변경을 담당합니다.
 * Redis 대기열·TTL 기능은 이번 세션 범위에서 제외하고, schedule_seats·seat_holds DB 상태만 기준으로 조회합니다.
 *
 * 삭제 로직은 DB 예외를 캐치하는 대신 삭제 전에 선점 이력을 미리 조회해서 판단합니다.
 * 별도의 트랜잭션 매니저나 REQUIRES_NEW 같은 수동 트랜잭션 제어는 쓰지 않고,
 * 일반적인 @Transactional 메서드 하나로 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class AdminSeatInventoryService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("MM.dd(E) HH:mm");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final PerformanceScheduleRepository scheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatHoldItemRepository seatHoldItemRepository;
    private final SeatRepository seatRepository;

    /** 공연 선택 시 "회차 선택" 드롭다운에 채울 옵션입니다. */
    @Transactional(readOnly = true)
    public List<SeatInventoryScheduleOptionDto> getScheduleOptions(Long performanceId) {
        if (performanceId == null) {
            return List.of();
        }
        return scheduleRepository.findByPerformanceIdOrderByStartsAtAsc(performanceId).stream()
                .map(s -> new SeatInventoryScheduleOptionDto(
                        s.getId(),
                        s.getStartsAt().format(DATE_TIME_FMT) + (s.getRoundNumber() != null ? " · " + s.getRoundNumber() + "회" : "")
                ))
                .collect(Collectors.toList());
    }

    /** 상단 통계 카드(전체/판매가능/선점중/판매완료/차단) 개수입니다. */
    @Transactional(readOnly = true)
    public SeatInventoryStatsDto getStats(Long scheduleId) {
        if (scheduleId == null) {
            return SeatInventoryStatsDto.empty();
        }
        long available = scheduleSeatRepository.countByScheduleIdAndStatus(scheduleId, ScheduleSeat.Status.AVAILABLE);
        long held = scheduleSeatRepository.countByScheduleIdAndStatus(scheduleId, ScheduleSeat.Status.HELD);
        long reserved = scheduleSeatRepository.countByScheduleIdAndStatus(scheduleId, ScheduleSeat.Status.RESERVED);
        long blocked = scheduleSeatRepository.countByScheduleIdAndStatus(scheduleId, ScheduleSeat.Status.BLOCKED);
        return new SeatInventoryStatsDto(available + held + reserved + blocked, available, held, reserved, blocked);
    }

    /** 좌석 등급(VIP/R/S 등)별 잔여석 현황입니다. */
    @Transactional(readOnly = true)
    public List<SeatInventoryGradeDto> getGradeBreakdown(Long scheduleId) {
        if (scheduleId == null) {
            return List.of();
        }
        List<ScheduleSeat> seats = scheduleSeatRepository.findWithSeatInfoByScheduleId(scheduleId);

        Map<Long, SeatGrade> gradeById = new LinkedHashMap<>();
        Map<Long, BigDecimal> priceByGrade = new LinkedHashMap<>();
        Map<Long, long[]> countsByGrade = new LinkedHashMap<>(); // [available, held, reserved, blocked]

        for (ScheduleSeat ss : seats) {
            SeatGrade grade = ss.getSeat().getSeatGrade();
            gradeById.putIfAbsent(grade.getId(), grade);
            priceByGrade.putIfAbsent(grade.getId(), ss.getPrice());
            long[] counts = countsByGrade.computeIfAbsent(grade.getId(), k -> new long[4]);
            counts[statusIndex(ss.getStatus())]++;
        }

        return gradeById.values().stream()
                .sorted((a, b) -> {
                    Integer oa = a.getSortOrder() != null ? a.getSortOrder() : 0;
                    Integer ob = b.getSortOrder() != null ? b.getSortOrder() : 0;
                    return oa.compareTo(ob);
                })
                .map(grade -> {
                    long[] c = countsByGrade.get(grade.getId());
                    long total = c[0] + c[1] + c[2] + c[3];
                    return new SeatInventoryGradeDto(grade.getId(), grade.getName(), grade.getDisplayColor(),
                            priceByGrade.get(grade.getId()), total, c[0], c[1], c[2], c[3]);
                })
                .collect(Collectors.toList());
    }

    /** 좌석 배치도(구역 > 행 > 좌석) 시각화 데이터입니다. */
    @Transactional(readOnly = true)
    public List<SeatInventorySectionDto> getSeatMap(Long scheduleId) {
        if (scheduleId == null) {
            return List.of();
        }
        List<ScheduleSeat> seats = scheduleSeatRepository.findWithSeatInfoByScheduleId(scheduleId);

        // 쿼리에서 이미 sectionName -> rowLabel -> seatNumber 순으로 정렬되어 있음
        Map<String, Map<String, List<SeatInventorySeatDto>>> grouped = new LinkedHashMap<>();

        for (ScheduleSeat ss : seats) {
            Seat seat = ss.getSeat();
            String sectionKey = seat.getSectionName() != null ? seat.getSectionName() : "구역 미지정";
            String rowKey = seat.getRowLabel() != null ? seat.getRowLabel() : "-";

            SeatInventorySeatDto dto = new SeatInventorySeatDto(
                    ss.getId(),
                    seat.getSeatNumber(),
                    ss.getStatus().name(),
                    statusText(ss.getStatus()),
                    seat.getSeatGrade().getName(),
                    seat.getSeatGrade().getDisplayColor(),
                    ss.getPrice()
            );

            grouped.computeIfAbsent(sectionKey, k -> new LinkedHashMap<>())
                    .computeIfAbsent(rowKey, k -> new ArrayList<>())
                    .add(dto);
        }

        List<SeatInventorySectionDto> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<SeatInventorySeatDto>>> sectionEntry : grouped.entrySet()) {
            List<SeatInventoryRowDto> rows = new ArrayList<>();
            for (Map.Entry<String, List<SeatInventorySeatDto>> rowEntry : sectionEntry.getValue().entrySet()) {
                rows.add(new SeatInventoryRowDto(rowEntry.getKey(), rowEntry.getValue()));
            }
            result.add(new SeatInventorySectionDto(sectionEntry.getKey(), rows));
        }
        return result;
    }

    /**
     * 현재 만료되지 않은 활성 선점(ACTIVE) 목록을 만료 임박 순으로 조회합니다.
     * 만료 처리 배치(Redis TTL 등)는 이번 세션 범위 밖이므로, expires_at 컬럼만 기준으로 판단합니다.
     */
    @Transactional(readOnly = true)
    public List<SeatInventoryHoldDto> getActiveHolds(Long scheduleId) {
        if (scheduleId == null) {
            return List.of();
        }
        List<SeatHold> holds = seatHoldRepository.findActiveHoldsByScheduleId(scheduleId, SeatHold.Status.ACTIVE);

        return holds.stream()
                .map(h -> new SeatInventoryHoldDto(
                        h.getId(),
                        maskMemberName(h.getMember().getName()),
                        seatHoldItemRepository.findBySeatHoldId(h.getId()).size(),
                        h.getExpiresAt().format(ISO_FMT)
                ))
                .collect(Collectors.toList());
    }

    /** AD08 "좌석 구성 관리" 모달 - 회차에 아직 등록되지 않은 물리 좌석 목록을 구역별로 묶어 조회합니다. */
    @Transactional(readOnly = true)
    public List<SeatInventoryUnassignedSectionDto> getUnassignedSeats(Long scheduleId) {
        if (scheduleId == null) {
            return List.of();
        }
        PerformanceSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다."));

        Map<String, List<SeatInventoryUnassignedSeatDto>> grouped = new LinkedHashMap<>();
        for (Seat seat : findMissingSeats(schedule)) {
            String sectionKey = seat.getSectionName() != null ? seat.getSectionName() : "구역 미지정";
            SeatInventoryUnassignedSeatDto dto = new SeatInventoryUnassignedSeatDto(
                    seat.getId(),
                    seat.getSectionName(),
                    seat.getRowLabel(),
                    seat.getSeatNumber(),
                    seat.getSeatGrade().getName(),
                    seat.getSeatGrade().getDisplayColor()
            );
            grouped.computeIfAbsent(sectionKey, k -> new ArrayList<>()).add(dto);
        }

        List<SeatInventoryUnassignedSectionDto> result = new ArrayList<>();
        for (Map.Entry<String, List<SeatInventoryUnassignedSeatDto>> entry : grouped.entrySet()) {
            result.add(new SeatInventoryUnassignedSectionDto(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /** AD08 "새 좌석 추가" 폼의 등급 선택지입니다. 회차의 좌석도(seat_chart)에 이미 쓰이고 있는 등급 목록에서 가져옵니다. */
    @Transactional(readOnly = true)
    public List<SeatInventoryGradeOptionDto> getGradeOptions(Long scheduleId) {
        if (scheduleId == null) {
            return List.of();
        }
        PerformanceSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다."));

        List<Seat> chartSeats = seatRepository
                .findBySeatChartIdOrderBySectionNameAscRowLabelAscSeatNumberAsc(schedule.getSeatChart().getId());

        Map<Long, SeatGrade> gradeById = new LinkedHashMap<>();
        for (Seat seat : chartSeats) {
            gradeById.putIfAbsent(seat.getSeatGrade().getId(), seat.getSeatGrade());
        }

        return gradeById.values().stream()
                .sorted((a, b) -> {
                    Integer oa = a.getSortOrder() != null ? a.getSortOrder() : 0;
                    Integer ob = b.getSortOrder() != null ? b.getSortOrder() : 0;
                    return oa.compareTo(ob);
                })
                .map(g -> new SeatInventoryGradeOptionDto(g.getId(), g.getName(), g.getDisplayColor()))
                .collect(Collectors.toList());
    }

    /**
     * AD08 "좌석 구성 관리" - 회차의 좌석도(seat_chart)에 속한 물리 좌석 중 아직 schedule_seats로
     * 등록되지 않은 좌석을 전부 AVAILABLE 상태로 일괄 생성합니다.
     * 가격은 아직 등급별 가격 연동 전이므로 0원으로 생성되며, 추후 raw_price_text 연동 시 갱신이 필요합니다.
     */
    @Transactional
    public int generateMissingScheduleSeats(Long scheduleId) {
        PerformanceSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다."));

        List<Seat> toCreateSeats = findMissingSeats(schedule);
        if (toCreateSeats.isEmpty()) {
            return 0;
        }
        List<ScheduleSeat> toCreate = toCreateSeats.stream()
                .map(seat -> ScheduleSeat.create(schedule, seat, BigDecimal.ZERO, "KRW"))
                .collect(Collectors.toList());
        scheduleSeatRepository.saveAll(toCreate);
        return toCreate.size();
    }

    /** AD08 "좌석 구성 관리" - 이미 좌석도(seat_chart)에 있는 물리 좌석 하나를 회차 좌석으로 개별 추가합니다. */
    @Transactional
    public void addSingleSeat(Long scheduleId, Long seatId) {
        PerformanceSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다."));
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다."));

        boolean alreadyExists = scheduleSeatRepository.findWithSeatInfoByScheduleId(scheduleId).stream()
                .anyMatch(ss -> ss.getSeat().getId().equals(seatId));
        if (alreadyExists) {
            throw new IllegalStateException("이미 등록된 좌석입니다.");
        }
        scheduleSeatRepository.save(ScheduleSeat.create(schedule, seat, BigDecimal.ZERO, "KRW"));
    }

    /**
     * AD08 "새 좌석 추가" - 좌석도(seat_chart)에 존재하지 않던 완전히 새로운 물리 좌석을 만들고,
     * 곧바로 이 회차의 좌석 재고(schedule_seats)에도 등록합니다.
     */
    @Transactional
    public void createNewSeat(Long scheduleId, String sectionName, String rowLabel, String seatNumber, Long gradeId) {
        PerformanceSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다."));

        List<Seat> chartSeats = seatRepository
                .findBySeatChartIdOrderBySectionNameAscRowLabelAscSeatNumberAsc(schedule.getSeatChart().getId());
        SeatGrade grade = chartSeats.stream()
                .map(Seat::getSeatGrade)
                .filter(g -> g.getId().equals(gradeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("등급을 찾을 수 없습니다."));

        boolean duplicate = chartSeats.stream().anyMatch(s ->
                java.util.Objects.equals(s.getSectionName(), sectionName)
                        && java.util.Objects.equals(s.getRowLabel(), rowLabel)
                        && java.util.Objects.equals(s.getSeatNumber(), seatNumber));
        if (duplicate) {
            throw new IllegalStateException("이미 같은 위치(구역·열·번호)의 좌석이 존재합니다.");
        }

        Seat newSeat = Seat.create(schedule.getSeatChart(), grade, Seat.ObjectType.SEAT,
                sectionName, rowLabel, seatNumber, 1, false, false);
        seatRepository.save(newSeat);
        scheduleSeatRepository.save(ScheduleSeat.create(schedule, newSeat, BigDecimal.ZERO, "KRW"));
    }

    /**
     * AD08 "좌석 구성 관리" - 회차 좌석 하나를 삭제합니다.
     * - 판매가능(AVAILABLE) 상태가 아니면 삭제할 수 없습니다.
     * - 과거에 한 번이라도 선점 이력(seat_hold_items)이 있으면 삭제할 수 없습니다(사전 조회로 미리 확인, DB 예외에 의존하지 않음).
     * - 삭제 후 이 물리 좌석을 다른 회차에서는 쓰지 않는다면, 좌석도(seats)에서도 완전히 삭제되어
     *   "미등록 좌석" 목록에도 다시 나타나지 않습니다.
     */
    @Transactional
    public void deleteScheduleSeat(Long scheduleSeatId) {
        ScheduleSeat seat = scheduleSeatRepository.findByIdForUpdate(scheduleSeatId)
                .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다."));
        if (seat.getStatus() != ScheduleSeat.Status.AVAILABLE) {
            throw new IllegalStateException("판매가능 상태의 좌석만 삭제할 수 있습니다.");
        }
        if (seatHoldItemRepository.countByScheduleSeatId(scheduleSeatId) > 0) {
            throw new IllegalStateException("이 좌석은 과거 선점 이력이 남아 있어 삭제할 수 없습니다.");
        }

        Long physicalSeatId = seat.getSeat().getId();
        scheduleSeatRepository.delete(seat);
        scheduleSeatRepository.flush();

        boolean usedInOtherSchedule = scheduleSeatRepository.existsBySeatId(physicalSeatId);
        if (!usedInOtherSchedule) {
            seatRepository.deleteById(physicalSeatId);
        }
    }

    /**
     * AD08 "좌석 구성 관리" - 여러 좌석을 한 번에 삭제합니다.
     * 좌석마다 사전 검증(상태·선점 이력)을 먼저 통과한 것만 삭제하므로, 하나가 조건에 안 맞아도
     * 나머지 삭제는 같은 트랜잭션 안에서 정상적으로 계속 진행됩니다.
     */
    @Transactional
    public SeatInventoryDeleteResultDto bulkDeleteScheduleSeats(List<Long> scheduleSeatIds) {
        if (scheduleSeatIds == null || scheduleSeatIds.isEmpty()) {
            return new SeatInventoryDeleteResultDto(0, 0);
        }
        int deleted = 0;
        int skipped = 0;
        for (Long id : scheduleSeatIds) {
            try {
                deleteScheduleSeat(id);
                deleted++;
            } catch (IllegalArgumentException | IllegalStateException e) {
                skipped++;
            }
        }
        return new SeatInventoryDeleteResultDto(deleted, skipped);
    }

    /** AD08 좌석 맵에서 여러 좌석을 선택해 한 번에 같은 상태로 변경합니다. */
    @Transactional
    public int bulkUpdateSeatStatus(List<Long> scheduleSeatIds, ScheduleSeat.Status status) {
        if (scheduleSeatIds == null || scheduleSeatIds.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Long id : scheduleSeatIds) {
            scheduleSeatRepository.findByIdForUpdate(id).ifPresent(seat -> seat.forceStatus(status));
            count++;
        }
        return count;
    }

    /** 회차의 좌석도에 속한 물리 좌석 중 아직 schedule_seats로 등록되지 않은 좌석 목록을 조회합니다. */
    private List<Seat> findMissingSeats(PerformanceSchedule schedule) {
        List<Seat> allSeats = seatRepository
                .findBySeatChartIdOrderBySectionNameAscRowLabelAscSeatNumberAsc(schedule.getSeatChart().getId());

        Set<Long> existingSeatIds = scheduleSeatRepository.findWithSeatInfoByScheduleId(schedule.getId()).stream()
                .map(ss -> ss.getSeat().getId())
                .collect(Collectors.toCollection(HashSet::new));

        return allSeats.stream()
                .filter(seat -> !existingSeatIds.contains(seat.getId()))
                .collect(Collectors.toList());
    }

    /** 회원명 마스킹(예: "관리자" -> "관리***"). 두 글자 이하이면 마지막 한 글자만 마스킹합니다. */
    private String maskMemberName(String name) {
        if (name == null || name.isBlank()) {
            return "회원***";
        }
        int visible = name.length() <= 2 ? name.length() - 1 : 2;
        if (visible < 1) visible = 1;
        return name.substring(0, visible) + "***";
    }

    private int statusIndex(ScheduleSeat.Status status) {
        return switch (status) {
            case AVAILABLE -> 0;
            case HELD -> 1;
            case RESERVED -> 2;
            case BLOCKED -> 3;
        };
    }

    private String statusText(ScheduleSeat.Status status) {
        return switch (status) {
            case AVAILABLE -> "판매가능";
            case HELD -> "선점중";
            case RESERVED -> "판매완료";
            case BLOCKED -> "차단";
        };
    }
}