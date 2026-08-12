package kr.co.stageon.payment.repository;

import kr.co.stageon.payment.domain.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 결제별 환불 이력 조회 DAO입니다. */
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByPaymentIdOrderByRequestedAtDesc(Long paymentId);
    List<Refund> findByPaymentIdInOrderByRequestedAtDesc(List<Long> paymentIds);
    long countByStatus(Refund.Status status);
}