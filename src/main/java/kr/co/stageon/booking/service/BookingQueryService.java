package kr.co.stageon.booking.service;

import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.domain.SeatHoldItem;
import kr.co.stageon.booking.dto.ReservationDetailResponse;
import kr.co.stageon.booking.dto.ReservationResponse;
import kr.co.stageon.booking.dto.SeatResponse;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.booking.dto.PaymentSummaryDto;
import kr.co.stageon.booking.repository.SeatHoldItemRepository;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import kr.co.stageon.booking.repository.ReservationSeatRepository;
import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.payment.repository.PaymentRepository;

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
import java.time.LocalDateTime;

/** 좌석 현황과 사용자 예매내역을 읽기 전용으로 제공합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingQueryService {

    private final ScheduleSeatRepository scheduleSeatRepository;
    private final ReservationRepository reservationRepository;
    private final RedissonClient redissonClient;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatHoldItemRepository seatHoldItemRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final PaymentRepository paymentRepository;

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

    public PaymentSummaryDto getPaymentSummaryInfo(Long seatHoldId) {
        SeatHold seatHold = seatHoldRepository.findById(seatHoldId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 선점 내역입니다. ID: " + seatHoldId));

        String performanceTitle = seatHold.getSchedule().getPerformance().getTitle();

        List<SeatHoldItem> items = seatHoldItemRepository.findBySeatHoldId(seatHoldId);
        BigDecimal totalAmount = items.stream()
                .map(item -> item.getScheduleSeat().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PaymentSummaryDto(totalAmount, performanceTitle);
    }

    public ReservationDetailResponse getReservationDetail(Long reservationId) {

        // 1. DB에서 진짜 예매 내역 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예매 내역을 찾을 수 없습니다."));

        // 2. 예매한 진짜 좌석 목록 조회
        List<ReservationDetailResponse.ReservedSeatItem> seats =
                reservationSeatRepository.findByReservationIdOrderByIdAsc(reservationId)
                        .stream()
                        .map(seat -> new ReservationDetailResponse.ReservedSeatItem(
                                seat.getId(),                  // 추가
                                seat.getCapturedGradeName(),
                                seat.getCapturedSectionName(),
                                seat.getCapturedRowLabel(),
                                seat.getCapturedSeatNumber(),
                                seat.getCapturedUnitPrice(),
                                seat.getStatus().name()
                        ))
                        .toList();

        // 3. 결제 내역 조회 (가장 최근 결제 1건)
        Payment latestPayment = paymentRepository
                .findByReservationIdOrderByRequestedAtDesc(reservationId)
                .stream()
                .findFirst()
                .orElse(null);

        var schedule = reservation.getSchedule();
        var performance = schedule.getPerformance();
        var hall = schedule.getVenueHall();
        var venue = hall.getVenue();

        return new ReservationDetailResponse(
                reservation.getId(),
                reservation.getBookingNumber(),
                reservation.getMember().getId(),
                // 공연 상세 페이지 링크에 사용할 ID
                performance.getId(),
                performance.getTitle(),
                performance.getPosterUrl(),
                schedule.getStartsAt(),
                schedule.getRoundNumber(),
                venue.getName(),
                hall.getName(),
                venue.getAddress(),
                reservation.getStatus().name(),
                reservation.getReceiveMethod().name(),
                reservation.getSeatAmount(),
                reservation.getFeeAmount(),
                reservation.getDiscountAmount(),
                reservation.getTotalAmount(),
                reservation.getReservedAt(),
                reservation.getCancelledAt(),
                reservation.getCancelReason(),
                latestPayment == null ? BigDecimal.ZERO : latestPayment.getCancelAmount(),
                latestPayment == null ? null : latestPayment.getPayMethod().name(),
                latestPayment == null ? null : latestPayment.getStatus().name(),
                latestPayment == null ? null : latestPayment.getRequestedAt(),
                latestPayment == null ? null : latestPayment.getProcessedAt(),
                seats
        );
    }

}