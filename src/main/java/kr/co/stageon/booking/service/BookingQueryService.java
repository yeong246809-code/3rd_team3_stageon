package kr.co.stageon.booking.service;

import kr.co.stageon.booking.dto.ReservationResponse;
import kr.co.stageon.booking.dto.SeatResponse;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/** 좌석 현황과 사용자 예매내역을 읽기 전용으로 제공합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingQueryService {

    private final ScheduleSeatRepository scheduleSeatRepository;
    private final ReservationRepository reservationRepository;

    public List<SeatResponse> findSeats(Long scheduleId) {
        return scheduleSeatRepository
                .findByScheduleIdOrderBySeatSectionNameAscSeatRowLabelAscSeatSeatNumberAsc(scheduleId)
                .stream().map(SeatResponse::from).toList();
    }

    public Map<String, List<SeatResponse>> findGroupedSeats(Long scheduleId) {
        return findSeats(scheduleId).stream()
                .collect(Collectors.groupingBy(SeatResponse::section));
    }

    public Optional<ReservationResponse> findReservation(Long reservationId) {
        return reservationRepository.findById(reservationId).map(ReservationResponse::from);
    }

    public List<ReservationResponse> findMemberReservations(Long memberId) {
        return reservationRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream().map(ReservationResponse::from).toList();
    }

    /**
     * 마이페이지의 '다가오는 공연' 영역에서 사용할
     * 가장 가까운 공연 예매 1건을 조회합니다.
     *
     * 조건
     * 1. 현재 로그인 회원의 예매일 것
     * 2. 예매 상태가 RESERVED일 것
     * 3. 공연 시작 시간이 현재 시각 이후일 것
     * 4. 그중 가장 빨리 시작하는 공연 1건
     */
    public Optional<ReservationResponse> findNearestUpcomingReservation(Long memberId) {

        LocalDateTime now = LocalDateTime.now();
        return findMemberReservations(memberId).stream()
                // 예매가 최종 확정된 건만 사용
                .filter(reservation ->
                        "RESERVED".equals(reservation.status())
                )
                // 공연 시작 시간이 현재보다 미래인 것만 사용
                .filter(reservation ->
                        reservation.startsAt() != null
                                && reservation.startsAt().isAfter(now)
                )

                // 공연 시작 시간이 가장 가까운 순으로 정렬
                .min((r1, r2) ->
                        r1.startsAt().compareTo(r2.startsAt())
                );
    }

    public record ScheduleSummaryResponse(String performanceTitle, java.time.LocalDateTime scheduleTime) {}

    public ScheduleSummaryResponse findScheduleSummary(Long scheduleId) {
        return scheduleSeatRepository
                .findByScheduleIdOrderBySeatSectionNameAscSeatRowLabelAscSeatSeatNumberAsc(scheduleId)
                .stream()
                .findFirst()
                .map(scheduleSeat -> {
                    var schedule = scheduleSeat.getSchedule();
                    return new ScheduleSummaryResponse(
                            schedule.getPerformance().getTitle(),
                            schedule.getStartsAt()
                    );
                })
                .orElse(null);
    }

    // 좌석 등급표를 프론트로 넘겨주기 위한 전용 DTO
    public record SeatGradeSummary(String name, BigDecimal price, String displayColor) {}

    // 기존 findSeats를 활용해 고유한 좌석 등급(가격, 색상) 목록만 추출하는 메서드
    public List<SeatGradeSummary> findSeatGrades(Long scheduleId) {
        return findSeats(scheduleId).stream()
                .map(seat -> new SeatGradeSummary(
                        seat.grade(),
                        seat.price(),
                        seat.displayColor()
                ))
                .distinct()
                .toList();
    }
}