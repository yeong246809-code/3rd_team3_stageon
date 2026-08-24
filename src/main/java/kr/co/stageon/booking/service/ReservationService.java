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
    public Long confirmReservation(
            PaymentRequest request, String paymentKey, String orderId, BigDecimal amount,
            Payment.PayMethod payMethod, Payment.Status paymentStatus,
            String vbankNum, String vbankName, LocalDateTime vbankDueDate) {

        // 1. 선점 내역(SeatHold) 상태 변경
        SeatHold seatHold = seatHoldRepository.findById(request.seatHoldId())
                .orElseThrow(() -> new IllegalArgumentException("선점 내역을 찾을 수 없습니다."));
        seatHold.complete();

        // 1-2. 공연 하루 전날부터 무통장입금 불가
        if (payMethod == Payment.PayMethod.VBANK) {
            java.time.LocalDate performanceDate = seatHold.getSchedule().getStartsAt().toLocalDate();
            java.time.LocalDate today = java.time.LocalDate.now();
            if (!today.isBefore(performanceDate.minusDays(1))) {
                throw new IllegalStateException("공연일 하루 전부터는 가상계좌 결제를 이용할 수 없습니다.");
            }
        }

        List<SeatHoldItem> holdItems = seatHoldItemRepository.findBySeatHoldIdOrderByIdAsc(seatHold.getId());
        // 2. 예매(Reservation) 생성
        Reservation reservation = Reservation.create(
                orderId,
                seatHold.getMember(),
                seatHold.getSchedule(),
                holdItems.size(),
                seatHold,
                Reservation.ReceiveMethod.MOBILE,
                amount,
                amount
        );

        // 💡 3. 가상계좌(READY)인 경우, 예매 상태를 PENDING(입금 대기)으로 변경
        if (paymentStatus == Payment.Status.READY) {
            reservation.markAsPending();
        }
        reservationRepository.save(reservation);

        // 3-1. 사용자가 선택한 좌석을 예매 좌석으로 저장
        // READY(결제 대기) 상태에서도 좌석은 예매에 연결되어 있어야 합니다.
        for (SeatHoldItem item : holdItems) {

            ReservationSeat reservationSeat =
                    ReservationSeat.create(
                            reservation,
                            item.getScheduleSeat()
                    );

            reservationSeatRepository.save(reservationSeat);
        }

        // 💡 4. 결제(Payment) 생성 (여기에 가상계좌 정보가 들어갑니다!)
        Payment payment = Payment.builder()
                .reservation(reservation)
                .paymentKey(paymentKey)
                .orderId(orderId)
                .provider(Payment.Provider.TOSSPAYMENTS) // Provider 필수
                .amount(amount)
                .payMethod(payMethod)
                .status(paymentStatus)
                .requestedAt(LocalDateTime.now()) // 요청 시간
                .vbankNum(vbankNum)
                .vbankName(vbankName)
                .vbankDueDate(vbankDueDate)
                .build();
        paymentRepository.save(payment);

        // 5. 개별 좌석(ScheduleSeat) 최종 확정 처리
        for (SeatHoldItem item : holdItems) {
            item.getScheduleSeat().reserve();
        }

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