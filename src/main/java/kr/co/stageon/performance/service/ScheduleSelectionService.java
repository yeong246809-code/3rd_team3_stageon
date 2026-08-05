package kr.co.stageon.performance.service;

import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.performance.dto.ScheduleSelectionResponse;
import kr.co.stageon.performance.dto.SeatGradeAvailabilityResponse;
import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [남수아 담당]
 * 날짜·회차 선택 화면 전용 서비스입니다.
 *
 * 기존 PerformanceQueryService는 수정하거나 사용하지 않습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleSelectionService {

    private final PerformanceScheduleRepository scheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;

    /**
     * 공연에 등록된 회차를 시작 일시 순서로 조회합니다.
     *
     * 각 회차에는 좌석 등급별 전체 좌석 수와
     * AVAILABLE 상태의 잔여 좌석 수가 포함됩니다.
     *
     * @param performanceId 공연 번호
     * @return 날짜·회차 선택 화면 전용 응답 목록
     */
    public List<ScheduleSelectionResponse> findScheduleSelections(
            Long performanceId
    ) {
        return scheduleRepository
                .findByPerformanceIdOrderByStartsAtAsc(performanceId)
                .stream()
                .map(schedule ->
                        ScheduleSelectionResponse.from(
                                schedule,
                                findSeatGradeAvailability(
                                        schedule.getId()
                                )
                        )
                )
                .toList();
    }

    /**
     * 특정 회차의 좌석을 등급별로 집계합니다.
     *
     * schedule_seats
     * → seats
     * → seat_grades
     *
     * 관계를 통해 실제 좌석 등급명을 확인합니다.
     */
    private List<SeatGradeAvailabilityResponse>
    findSeatGradeAvailability(Long scheduleId) {

        List<ScheduleSeat> scheduleSeats =
                scheduleSeatRepository
                        .findWithSeatInfoByScheduleId(scheduleId);

        Map<Long, SeatGradeCounter> counters =
                new LinkedHashMap<>();

        scheduleSeats.stream()
                .sorted(
                        Comparator.comparing(
                                scheduleSeat ->
                                        scheduleSeat
                                                .getSeat()
                                                .getSeatGrade()
                                                .getSortOrder()
                        )
                )
                .forEach(scheduleSeat -> {

                    var seatGrade = scheduleSeat
                            .getSeat()
                            .getSeatGrade();

                    SeatGradeCounter counter =
                            counters.computeIfAbsent(
                                    seatGrade.getId(),
                                    ignored ->
                                            new SeatGradeCounter(
                                                    seatGrade.getId(),
                                                    seatGrade.getName(),
                                                    seatGrade.getDisplayColor(),
                                                    seatGrade.getSortOrder()
                                            )
                            );

                    // 해당 등급의 전체 좌석 수
                    counter.totalSeatCount++;

                    // AVAILABLE 상태만 잔여석으로 계산
                    if (scheduleSeat.getStatus()
                            == ScheduleSeat.Status.AVAILABLE) {

                        counter.availableSeatCount++;
                    }
                });

        return counters.values()
                .stream()
                .map(SeatGradeCounter::toResponse)
                .toList();
    }

    /**
     * 좌석 등급별 수량을 계산하기 위한 내부 객체입니다.
     */
    private static class SeatGradeCounter {

        private final Long gradeId;
        private final String gradeName;
        private final String displayColor;
        private final Integer sortOrder;

        private long totalSeatCount;
        private long availableSeatCount;

        private SeatGradeCounter(
                Long gradeId,
                String gradeName,
                String displayColor,
                Integer sortOrder
        ) {
            this.gradeId = gradeId;
            this.gradeName = gradeName;
            this.displayColor = displayColor;
            this.sortOrder = sortOrder;
        }

        private SeatGradeAvailabilityResponse toResponse() {
            return new SeatGradeAvailabilityResponse(
                    gradeId,
                    gradeName,
                    displayColor,
                    sortOrder,
                    totalSeatCount,
                    availableSeatCount
            );
        }
    }
}