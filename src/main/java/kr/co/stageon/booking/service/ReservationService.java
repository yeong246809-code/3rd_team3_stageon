package kr.co.stageon.booking.service;


import kr.co.stageon.booking.domain.*;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.ReservationSeatRepository;
import kr.co.stageon.booking.repository.SeatHoldItemRepository;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import kr.co.stageon.booking.dto.PaymentRequest;
import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.payment.repository.PaymentRepository;
import kr.co.stageon.payment.repository.RefundRepository;
import kr.co.stageon.payment.service.PaymentService;
import kr.co.stageon.ticket.domain.Ticket;
import kr.co.stageon.ticket.repository.TicketRepository;
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
    private final TicketRepository ticketRepository;
    private final RefundRepository refundRepository;

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

            // 🚨 예약 좌석이 저장된 직후, 수령 방법이 모바일이면 티켓 엔티티 생성
            if (reservation.getReceiveMethod() == Reservation.ReceiveMethod.MOBILE) {
                String qrToken = java.util.UUID.randomUUID().toString().replace("-", "");

                Ticket ticket = Ticket.builder()
                        .reservationSeat(reservationSeat) // 방금 위에서 만든 reservationSeat 사용
                        .ticketNumber(reservation.getBookingNumber())
                        .qrTokenHash(qrToken)
                        .build();

                ticketRepository.save(ticket);
            }
        }

        seatHold.complete();

        return reservation.getId();

    }

    /**
     * 선택된 좌석에 대한 부분/전체 취소 로직을 수행합니다.
     */
    @Transactional
    public void cancelSeats(Long reservationId, List<Long> seatIdsToCancel, String cancelReason,
                            String refundBank, String refundAccountNumber, String refundHolderName) {

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

        // 4. 취소 금액 계산 (수수료 공제 전 원래 가격)
        BigDecimal originalCancelAmount = targetSeats.stream()
                .map(ReservationSeat::getCapturedUnitPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 💡 [수수료 로직 추가] 공연 시작일 기준으로 실제 환불 금액 계산
        LocalDateTime performanceStartAt = reservation.getSchedule().getStartsAt();
        kr.co.stageon.payment.domain.CancelFeePolicy policy =
                kr.co.stageon.payment.domain.CancelFeePolicy.getPolicy(LocalDateTime.now(), performanceStartAt);

        BigDecimal actualRefundAmount = policy.calculateRefundAmount(originalCancelAmount);
        String detailCancelReason = cancelReason + " (수수료 " + (int)(policy.getFeeRate() * 100) + "% 적용)";

        // 5. 토스페이먼츠 취소 API 호출 (실제 환불 처리)
        boolean isTossPayment = payment.getProvider() == Payment.Provider.TOSSPAYMENTS;
        boolean isSuccess = payment.getStatus() == Payment.Status.SUCCESS;
        boolean isVbankReady = payment.getPayMethod() == Payment.PayMethod.VBANK && payment.getStatus() == Payment.Status.READY;

        String pgTid = null;
        if (isTossPayment && (isSuccess || isVbankReady) && payment.getPaymentKey() != null) {
            pgTid = paymentService.cancelTossPayment(payment, actualRefundAmount, detailCancelReason,
                    refundBank, refundAccountNumber, refundHolderName);
        }

        // 💡 [환불 내역 저장 추가] refunds 테이블에 취소 이력 기록!
        kr.co.stageon.payment.domain.Refund refund = kr.co.stageon.payment.domain.Refund.createCompleted(
                payment,
                actualRefundAmount,
                kr.co.stageon.payment.domain.Refund.Category.USER_CANCEL,
                detailCancelReason,
                pgTid
        );
        refundRepository.save(refund);

        // 6. DB 데이터 업데이트 (좌석 해제 및 상태 변경)
        for (ReservationSeat seat : targetSeats) {
            seat.getScheduleSeat().release();
            seat.cancel();
            ticketRepository.findByReservationSeatId(seat.getId())
                    .ifPresent(Ticket::cancel);
        }

        // 7. 전체 취소 vs 부분 취소 상태값 변경
        long remainingCount = reservationSeatRepository.findByReservationIdOrderByIdAsc(reservationId).stream()
                .filter(seat -> seat.getStatus() != ReservationSeat.Status.CANCELLED) // 취소된 좌석 제외
                .count();

        if (remainingCount == 0) {
            // 남은 좌석이 0개면 전체 취소
            reservation.cancel(detailCancelReason);
            payment.markCancelled(actualRefundAmount);
        } else {
            // 남은 좌석이 있으면 부분 취소
            reservation.deductAmount(originalCancelAmount);
            payment.addCancelAmount(actualRefundAmount);
            reservation.decreaseTicketCount(targetSeats.size());
        }
    }
}