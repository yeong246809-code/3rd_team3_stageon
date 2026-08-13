package kr.co.stageon.booking.service;

import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.booking.dto.ReservationDetailResponse;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.ReservationSeatRepository;
import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 마이페이지 예매 상세 화면 전용 조회 서비스입니다.
 * 예매, 좌석, 최신 결제 정보를 읽기만 하므로 기존 결제 처리 로직과 분리합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationDetailQueryService {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final PaymentRepository paymentRepository;

    /** 로그인 회원 본인의 예매 상세만 조회합니다. */
    public ReservationDetailResponse findDetail(Long reservationId, Long memberId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .filter(found -> found.getMember().getId().equals(memberId))
                .orElseThrow(() -> new IllegalArgumentException("예매 내역을 찾을 수 없습니다."));

        // 예매 당시 저장된 좌석명과 가격을 사용해 이후 좌석 정보가 바뀌어도 내역을 보존합니다.
        List<ReservationDetailResponse.ReservedSeatItem> seats =
                reservationSeatRepository.findByReservationIdOrderByIdAsc(reservationId)
                        .stream()
                        .map(seat -> new ReservationDetailResponse.ReservedSeatItem(
                                seat.getId(),                  // reservation_seats.id
                                seat.getCapturedGradeName(),
                                seat.getCapturedSectionName(),
                                seat.getCapturedRowLabel(),
                                seat.getCapturedSeatNumber(),
                                seat.getCapturedUnitPrice()
                        ))
                        .toList();

        // 여러 결제 시도가 있을 수 있으므로 가장 최근 요청 한 건을 상세 화면에 표시합니다.
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
                latestPayment == null ? null : latestPayment.getPayMethod().name(),
                latestPayment == null ? null : latestPayment.getStatus().name(),
                latestPayment == null ? null : latestPayment.getRequestedAt(),
                latestPayment == null ? null : latestPayment.getProcessedAt(),
                seats
        );
    }
}