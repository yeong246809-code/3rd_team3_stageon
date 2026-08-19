package kr.co.stageon.payment.repository;

import kr.co.stageon.payment.domain.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 결제별 환불 이력 및 관리자 "환불 관리" 화면 조회 DAO입니다. */
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByPaymentIdOrderByRequestedAtDesc(Long paymentId);
    List<Refund> findByPaymentIdInOrderByRequestedAtDesc(List<Long> paymentIds);
    long countByStatus(Refund.Status status);

    /** 관리자 "환불 관리" 목록 검색입니다. 파라미터가 null이면 해당 조건은 무시됩니다. */
    @Query("""
            SELECT rf FROM Refund rf
            JOIN FETCH rf.payment p
            JOIN FETCH p.reservation r
            JOIN FETCH r.member m
            JOIN FETCH r.schedule s
            JOIN FETCH s.performance perf
            WHERE (:status IS NULL OR rf.status = :status)
              AND (:category IS NULL OR rf.refundCategory = :category)
              AND (:keyword IS NULL OR m.name LIKE CONCAT('%', :keyword, '%') OR r.bookingNumber LIKE CONCAT('%', :keyword, '%'))
              AND (:fromDate IS NULL OR rf.requestedAt >= :fromDate)
              AND (:toDate IS NULL OR rf.requestedAt <= :toDate)
            ORDER BY rf.requestedAt DESC
            """)
    Page<Refund> search(@Param("status") Refund.Status status,
                        @Param("category") Refund.Category category,
                        @Param("keyword") String keyword,
                        @Param("fromDate") LocalDateTime fromDate,
                        @Param("toDate") LocalDateTime toDate,
                        Pageable pageable);

    /** 상세 모달용 조회입니다. */
    @Query("""
            SELECT rf FROM Refund rf
            JOIN FETCH rf.payment p
            JOIN FETCH p.reservation r
            JOIN FETCH r.member m
            JOIN FETCH r.schedule s
            JOIN FETCH s.performance perf
            WHERE rf.id = :id
            """)
    Optional<Refund> findByIdWithDetails(@Param("id") Long id);
}