package kr.co.stageon.payment.repository;

import kr.co.stageon.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 예매별 결제 및 멱등키 조회 DAO입니다. */
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReservationId(Long reservationId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
