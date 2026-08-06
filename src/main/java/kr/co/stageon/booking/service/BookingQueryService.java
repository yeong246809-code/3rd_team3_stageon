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