package kr.co.stageon.booking.service;


import kr.co.stageon.booking.domain.*;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.ReservationSeatRepository;
import kr.co.stageon.booking.repository.SeatHoldItemRepository;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import kr.co.stageon.booking.dto.PaymentRequest;
import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.payment.repository.PaymentRepository;
import kr.co.stageon.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final SeatHoldRepository seatHoldRepository;
    private final SeatHoldItemRepository seatHoldItemRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    /**
     * 결제 검증이 완료된 후, 실제 예매 데이터를 생성하고 DB에 저장합니다.
     */
    @Transactional
    public Long confirmReservation(PaymentRequest request, String paymentKey, String orderId,
                                   BigDecimal expectedAmount, Payment.PayMethod payMethod,
                                   Payment.Status paymentStatus, String vbankNum,
                                   String vbankName, LocalDateTime vbankDueDate) {

        // 1. 임시 선점(SeatHold) 내역 조회
        SeatHold seatHold = seatHoldRepository.findById(request.seatHoldId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 선점 내역입니다."));

        if (!seatHold.getHoldTokenHash().equals(request.holdTokenHash())) {
            throw new IllegalArgumentException("비정상적인 예매 요청입니다.");
        }

        List<SeatHoldItem> holdItems = seatHoldItemRepository.findBySeatHoldId(seatHold.getId());
        int ticketCount = holdItems.size();

        Reservation reservation = Reservation.create(
                orderId,
                seatHold.getMember(),
                seatHold.getSchedule(),
                ticketCount,
                seatHold,
                request.receiveMethod(),
                expectedAmount,
                expectedAmount
        );
        reservationRepository.save(reservation);

        Payment payment = Payment.builder()
                .reservation(reservation)
                .paymentKey(paymentKey)
                .orderId(orderId)
                .provider(Payment.Provider.TOSSPAYMENTS)
                .payMethod(payMethod)
                .amount(expectedAmount)
                .status(paymentStatus)
                .vbankNum(vbankNum)
                .vbankName(vbankName)
                .vbankDueDate(vbankDueDate)
                .requestedAt(LocalDateTime.now())
                .processedAt(paymentStatus == Payment.Status.SUCCESS ? LocalDateTime.now() : null)
                .build();
        paymentRepository.save(payment);

        for (SeatHoldItem item : holdItems) {
            ScheduleSeat scheduleSeat = item.getScheduleSeat();
            scheduleSeat.reserve();
            ReservationSeat reservationSeat = ReservationSeat.create(reservation, scheduleSeat);
            reservationSeatRepository.save(reservationSeat);
        }

        seatHold.complete();

        return reservation.getId();
    }

    /**
     * 선택된 좌석에 대한 부분/전체 취소 로직을 수행합니다.
     */
    @Transactional
    public void cancelSeats(Long reservationId, List<Long> seatIdsToCancel, String cancelReason) {

        // 1. 예약 정보 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));

        // 💡 2. reservationId를 이용해 가장 최근 결제 내역 조회 (ReservationDetailQueryService 참고)
        Payment payment = paymentRepository.findByReservationIdOrderByRequestedAtDesc(reservationId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("결제 내역을 찾을 수 없습니다."));

        // 3. 취소할 좌석들(ReservationSeat) 조회
        List<ReservationSeat> targetSeats = reservationSeatRepository.findAllById(seatIdsToCancel);
        if (targetSeats.isEmpty()) {
            throw new IllegalArgumentException("취소할 좌석이 없습니다.");
        }

        // 4. 취소 금액 계산
        BigDecimal totalCancelAmount = targetSeats.stream()
                .map(ReservationSeat::getCapturedUnitPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. 토스페이먼츠 취소 API 호출 (실제 환불 처리)
        if (payment.getPaymentKey() != null) {
            // 💡 payment.getPaymentKey() 대신 payment 객체 자체를 넘겨줍니다.
            paymentService.cancelTossPayment(payment, totalCancelAmount, cancelReason);
        }

        // 6. DB 데이터 업데이트 (좌석 해제 및 삭제)
        for (ReservationSeat seat : targetSeats) {
            seat.getScheduleSeat().release();
            reservationSeatRepository.delete(seat);
        }

        // 7. 전체 취소 vs 부분 취소 상태값 변경
        List<ReservationSeat> remainingSeats = reservationSeatRepository.findByReservationIdOrderByIdAsc(reservationId);

        if (remainingSeats.isEmpty()) {
            // 남은 좌석이 없으면 전체 취소
            reservation.cancel(cancelReason);
            payment.markCancelled(totalCancelAmount);
        } else {
            // 남은 좌석이 있으면 부분 취소
            reservation.deductAmount(totalCancelAmount);
            payment.addCancelAmount(totalCancelAmount);
        }
    }
}