package kr.co.stageon.booking.service;


import kr.co.stageon.booking.domain.*;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.ReservationSeatRepository;
import kr.co.stageon.booking.repository.SeatHoldItemRepository;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import kr.co.stageon.booking.dto.PaymentRequest;
import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.payment.repository.PaymentRepository;
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
}