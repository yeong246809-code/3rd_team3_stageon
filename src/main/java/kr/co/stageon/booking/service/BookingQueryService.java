package kr.co.stageon.booking.service;

import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.dto.ReservationResponse;
import kr.co.stageon.booking.dto.SeatResponse;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

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
    private final RedissonClient redissonClient;

    public List<SeatResponse> findSeats(Long scheduleId) {

        // 🚨 1. 현재 화면을 조회하고 있는 유저의 ID 가져오기
        String currentUserId = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            currentUserId = ((UserDetails) auth.getPrincipal()).getUsername();
        }
        final String loggedInUserId = currentUserId;

        return scheduleSeatRepository
                .findByScheduleIdOrderBySeatSectionNameAscSeatRowLabelAscSeatSeatNumberAsc(scheduleId)
                .stream()
                .map(scheduleSeat -> {
                    SeatResponse response = SeatResponse.from(scheduleSeat);

                    // 🚨 2. Redis 락의 '주인'이 누구인지 확인
                    RBucket<String> lockBucket = redissonClient.getBucket("seat:selecting:" + scheduleSeat.getId());
                    String lockOwner = lockBucket.get();

                    boolean isDbHeld = scheduleSeat.getStatus() != ScheduleSeat.Status.AVAILABLE;
                    boolean isRedisLocked = lockOwner != null;

                    // 락이 걸려있지만 그 주인이 '나' 인지 확인
                    boolean isMyLock = isRedisLocked && lockOwner.equals(loggedInUserId);

                    // 🚨 3. DB가 선점되었거나, (Redis 락이 걸려있는데 내 락이 아닌 경우)에만 HELD로 렌더링
                    if (isDbHeld || (isRedisLocked && !isMyLock)) {
                        return new SeatResponse(
                                response.id(), response.scheduleId(), response.section(), response.row(),
                                response.number(), response.grade(), response.displayColor(),
                                response.seatsioObjectKey(), response.objectType(), response.price(),
                                response.currency(), "HELD", response.accessible()
                        );
                    }

                    return response;
                })
                .toList();
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