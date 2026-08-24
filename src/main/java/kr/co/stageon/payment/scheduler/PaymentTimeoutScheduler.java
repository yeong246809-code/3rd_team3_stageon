package kr.co.stageon.payment.scheduler;

import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.booking.domain.ReservationSeat;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.ReservationSeatRepository;
import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final PaymentRepository paymentRepository;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void cancelExpiredVirtualAccounts() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 상태가 PENDING이고 만료시간이 지난 예매 조회
        List<Reservation> expiredReservations = reservationRepository.findExpiredPendingReservations(Reservation.Status.PENDING, now);

        for (Reservation reservation : expiredReservations) {
            try {
                // 2. 예매 상태 변경 -> CANCELLED
                reservation.cancel("결제 기한 만료(미입금)");

                // 3. 결제 상태 변경 -> CANCELED (READY 상태 전용 취소 메서드 사용!)
                List<Payment> payments = paymentRepository.findByReservationIdOrderByRequestedAtDesc(reservation.getId());
                if (!payments.isEmpty()) {
                    Payment payment = payments.get(0);
                    if (payment.getStatus() == Payment.Status.READY) {
                        payment.markUnpaidCanceled();
                    }
                }

                // 4. 묶여있던 좌석 해제 -> AVAILABLE
                List<ReservationSeat> seats = reservationSeatRepository.findByReservationIdOrderByIdAsc(reservation.getId());
                for (ReservationSeat seat : seats) {
                    seat.getScheduleSeat().release();
                    seat.cancel();
                }

                log.info("결제 기한 만료 자동 취소 및 좌석 해제 완료. reservationId={}", reservation.getId());
            } catch (Exception e) {
                log.error("결제 기한 만료 자동 취소 실패. reservationId={}", reservation.getId(), e);
            }
        }
    }
}