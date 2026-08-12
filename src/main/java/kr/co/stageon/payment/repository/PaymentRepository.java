package kr.co.stageon.payment.repository;

import kr.co.stageon.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 예매별 결제 및 멱등키 조회 DAO입니다. */
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByReservationIdOrderByRequestedAtDesc(Long reservationId);
    List<Payment> findByReservationIdInOrderByRequestedAtDesc(List<Long> reservationIds);
    //Optional<Payment> findByProviderAndIdempotencyKey(Payment.Provider provider, String idempotencyKey);

    /** 대시보드 - 기간 내 특정 상태(예: SUCCESS) 결제 건수 */
    long countByStatusAndRequestedAtBetween(Payment.Status status, LocalDateTime start, LocalDateTime end);

    /** 대시보드 - 기간 내 전체 결제 시도 건수 (성공률 분모) */
    long countByRequestedAtBetween(LocalDateTime start, LocalDateTime end);

    Optional<Payment> findByOrderId(String orderId);

    /** AD09 통계 - 전체 기간 성공 결제 총 매출 */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal sumSuccessAmount();

    /** AD09 상세/취소 - 예매의 성공(SUCCESS) 결제 1건 조회 */
    Optional<Payment> findFirstByReservationIdAndStatusOrderByRequestedAtDesc(Long reservationId, Payment.Status status);
}