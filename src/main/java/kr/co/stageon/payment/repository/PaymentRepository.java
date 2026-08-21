package kr.co.stageon.payment.repository;

import kr.co.stageon.payment.domain.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 예매별 결제 및 멱등키 조회 DAO입니다. */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** 정산 화면 - 공연별 매출 집계 프로젝션 */
    interface PerformanceRevenueProjection {
        Long getPerformanceId();
        String getTitle();
        Long getPaymentCount();
        BigDecimal getGrossAmount();
        BigDecimal getRefundAmount();
    }

    /** 정산 화면 - 공연 상세(회차별) 매출 집계 프로젝션 */
    interface ScheduleRevenueProjection {
        Long getScheduleId();
        LocalDateTime getStartsAt();
        Long getPaymentCount();
        BigDecimal getGrossAmount();
        BigDecimal getRefundAmount();
    }

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

    /** 환불 관리 - 수동 환불 대상 선택용, 특정 회원의 특정 상태 결제 목록(예매·공연정보 포함)입니다. */
    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.reservation r
            JOIN FETCH r.schedule s
            JOIN FETCH s.performance perf
            WHERE r.member.id = :memberId AND p.status = :status
            ORDER BY p.requestedAt DESC
            """)
    List<Payment> findByMemberIdAndStatusOrderByRequestedAtDesc(@Param("memberId") Long memberId,
                                                                @Param("status") Payment.Status status);

    /** 정산 화면 - 상단 통계 카드용, 기간 내 성공 결제 총 매출(취소분 포함 총액) */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
            WHERE p.status = 'SUCCESS'
              AND (:fromDate IS NULL OR p.processedAt >= :fromDate)
              AND (:toDate IS NULL OR p.processedAt <= :toDate)
            """)
    BigDecimal sumGrossAmount(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    /** 정산 화면 - 상단 통계 카드용, 기간 내 성공 결제의 누적 환불액 */
    @Query("""
            SELECT COALESCE(SUM(p.cancelAmount), 0) FROM Payment p
            WHERE p.status = 'SUCCESS'
              AND (:fromDate IS NULL OR p.processedAt >= :fromDate)
              AND (:toDate IS NULL OR p.processedAt <= :toDate)
            """)
    BigDecimal sumRefundAmount(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    /** 정산 화면 - 상단 통계 카드용, 기간 내 성공 결제 건수 */
    @Query("""
            SELECT COUNT(p) FROM Payment p
            WHERE p.status = 'SUCCESS'
              AND (:fromDate IS NULL OR p.processedAt >= :fromDate)
              AND (:toDate IS NULL OR p.processedAt <= :toDate)
            """)
    long countSuccess(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    /** 정산 화면 - 공연별 매출 목록(검색·기간·페이지네이션) */
    @Query(value = """
            SELECT s.performance.id AS performanceId,
                   s.performance.title AS title,
                   COUNT(p) AS paymentCount,
                   COALESCE(SUM(p.amount), 0) AS grossAmount,
                   COALESCE(SUM(p.cancelAmount), 0) AS refundAmount
            FROM Payment p
            JOIN p.reservation r
            JOIN r.schedule s
            WHERE p.status = 'SUCCESS'
              AND (:fromDate IS NULL OR p.processedAt >= :fromDate)
              AND (:toDate IS NULL OR p.processedAt <= :toDate)
              AND (:keyword IS NULL OR LOWER(s.performance.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            GROUP BY s.performance.id, s.performance.title
            """,
            countQuery = """
            SELECT COUNT(DISTINCT s.performance.id)
            FROM Payment p
            JOIN p.reservation r
            JOIN r.schedule s
            WHERE p.status = 'SUCCESS'
              AND (:fromDate IS NULL OR p.processedAt >= :fromDate)
              AND (:toDate IS NULL OR p.processedAt <= :toDate)
              AND (:keyword IS NULL OR LOWER(s.performance.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<PerformanceRevenueProjection> searchPerformanceRevenue(@Param("keyword") String keyword,
                                                                @Param("fromDate") LocalDateTime fromDate,
                                                                @Param("toDate") LocalDateTime toDate,
                                                                Pageable pageable);

    /** 정산 화면 - 공연 상세 모달용, 특정 공연의 회차별 매출 breakdown */
    @Query("""
            SELECT s.id AS scheduleId,
                   s.startsAt AS startsAt,
                   COUNT(p) AS paymentCount,
                   COALESCE(SUM(p.amount), 0) AS grossAmount,
                   COALESCE(SUM(p.cancelAmount), 0) AS refundAmount
            FROM Payment p
            JOIN p.reservation r
            JOIN r.schedule s
            WHERE s.performance.id = :performanceId
              AND p.status = 'SUCCESS'
              AND (:fromDate IS NULL OR p.processedAt >= :fromDate)
              AND (:toDate IS NULL OR p.processedAt <= :toDate)
            GROUP BY s.id, s.startsAt
            ORDER BY s.startsAt ASC
            """)
    List<ScheduleRevenueProjection> findScheduleRevenueByPerformance(@Param("performanceId") Long performanceId,
                                                                     @Param("fromDate") LocalDateTime fromDate,
                                                                     @Param("toDate") LocalDateTime toDate);
}