package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.SeatInventoryGradeDto;
import kr.co.stageon.admin.dto.SeatInventoryHoldDto;
import kr.co.stageon.admin.dto.SeatInventoryRowDto;
import kr.co.stageon.admin.dto.SeatInventorySeatDto;
import kr.co.stageon.admin.dto.SeatInventorySectionDto;
import kr.co.stageon.admin.dto.SeatInventoryScheduleOptionDto;
import kr.co.stageon.admin.dto.SeatInventoryStatsDto;
import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.booking.repository.SeatHoldItemRepository;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import kr.co.stageon.venue.domain.Seat;
import kr.co.stageon.venue.domain.SeatGrade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AD08 "좌석 재고·선점 현황" 화면의 통계/등급별 잔여석/좌석 배치도/활성 선점 조회를 담당합니다.
 * Redis 대기열·TTL 기능은 이번 세션 범위에서 제외하고, schedule_seats·seat_holds DB 상태만 기준으로 조회합니다.
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
        Map<Long, long[]> countsByGrade = new LinkedHashMap<>(); // [available, held, reserved, blocked]

        for (ScheduleSeat ss : seats) {
            SeatGrade grade = ss.getSeat().getSeatGrade();
            gradeById.putIfAbsent(grade.getId(), grade);
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
                            total, c[0], c[1], c[2], c[3]);
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