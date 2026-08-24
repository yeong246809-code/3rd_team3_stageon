package kr.co.stageon.booking.repository;

import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.performance.domain.Performance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 예매 내역과 예매번호 조회 DAO입니다. */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    interface PerformanceRankingProjection {
        Long getPerformanceId();
        String getTitle();
        String getGenre();
        String getPosterUrl();
        long getTicketCount();
    }

    Optional<Reservation> findByBookingNumber(String bookingNumber);
    List<Reservation> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<Reservation> findByStatusOrderByCreatedAtDesc(Reservation.Status status);

    /** 대시보드 - 특정 상태 & 기간(예: 오늘) 기준 예매 건수 */
    long countByStatusAndReservedAtBetween(Reservation.Status status, LocalDateTime start, LocalDateTime end);

    /** AD09 통계 - 상태 무관 기간(예: 오늘) 기준 예매 건수 */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /** AD09 통계 - 특정 상태(예: CANCELLED) 전체 건수 */
    long countByStatus(Reservation.Status status);

    /** 대시보드 - 최근 예매 N건 (공연명 표시를 위해 schedule/performance까지 함께 조회) */
    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.schedule s
            JOIN FETCH s.performance p
            ORDER BY r.createdAt DESC
            """)
    List<Reservation> findRecentWithPerformance(Pageable pageable);

    /** 홈 랭킹용으로 최근 7일간 확정된 예매 좌석 수를 공연별로 집계합니다. */
    @Query("""
            SELECT p.id AS performanceId,
                   p.title AS title,
                   p.genre AS genre,
                   p.posterUrl AS posterUrl,
                   COUNT(rs.id) AS ticketCount
            FROM ReservationSeat rs
            JOIN rs.reservation r
            JOIN r.schedule s
            JOIN s.performance p
            WHERE r.status = :reservationStatus
              AND r.reservedAt >= :from
              AND p.draft = false
              AND p.status IN :performanceStatuses
              AND p.endDate >= :today
            GROUP BY p.id, p.title, p.genre, p.posterUrl
            ORDER BY p.genre ASC, COUNT(rs.id) DESC, p.id ASC
            """)
    List<PerformanceRankingProjection> findPerformanceRankings(
            @Param("reservationStatus") Reservation.Status reservationStatus,
            @Param("from") LocalDateTime from,
            @Param("performanceStatuses") List<Performance.Status> performanceStatuses,
            @Param("today") java.time.LocalDate today
    );

    @Query("SELECT r FROM Reservation r JOIN FETCH r.schedule s JOIN FETCH s.performance p WHERE r.id = :reservationId")
    Optional<Reservation> findByIdWithDetails(@Param("reservationId") Long reservationId);

    /** AD09 "예매·주문 조회" 목록 검색입니다. 파라미터가 null이면 해당 조건은 무시됩니다. */
    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.member m
            JOIN FETCH r.schedule s
            JOIN FETCH s.performance p
            WHERE (:performanceId IS NULL OR p.id = :performanceId)
              AND (:scheduleId IS NULL OR s.id = :scheduleId)
              AND (:status IS NULL OR r.status = :status)
              AND (:keyword IS NULL OR m.name LIKE CONCAT('%', :keyword, '%') OR r.bookingNumber LIKE CONCAT('%', :keyword, '%'))
              AND (:fromDate IS NULL OR r.createdAt >= :fromDate)
              AND (:toDate IS NULL OR r.createdAt <= :toDate)
              AND (:paymentStatus IS NULL OR EXISTS (
                    SELECT 1 FROM Payment pay WHERE pay.reservation = r AND pay.status = :paymentStatus
              ))
            ORDER BY r.createdAt DESC
            """)
    Page<Reservation> search(@Param("performanceId") Long performanceId,
                             @Param("scheduleId") Long scheduleId,
                             @Param("status") Reservation.Status status,
                             @Param("paymentStatus") Payment.Status paymentStatus,
                             @Param("keyword") String keyword,
                             @Param("fromDate") LocalDateTime fromDate,
                             @Param("toDate") LocalDateTime toDate,
                             Pageable pageable);
           
    @Query("SELECT COALESCE(SUM(r.ticketCount), 0) FROM Reservation r WHERE r.member.id = :memberId AND r.schedule.id = :scheduleId AND r.status = :status")
    Integer sumTicketCountByMemberIdAndScheduleId(
            @Param("memberId") Long memberId,
            @Param("scheduleId") Long scheduleId,
            @Param("status") Reservation.Status status
    );

    /** 한 회원이 동일 공연에서 이미 예매한 전체 티켓 수입니다. */
    @Query("""
            SELECT COALESCE(SUM(r.ticketCount), 0)
            FROM Reservation r
            WHERE r.member.id = :memberId
              AND r.schedule.performance.id = :performanceId
              AND r.status = :status
            """)
    Integer sumTicketCountByMemberIdAndPerformanceId(
            @Param("memberId") Long memberId,
            @Param("performanceId") Long performanceId,
            @Param("status") Reservation.Status status
    );

    @Query("SELECT r FROM Reservation r WHERE r.status = :status AND r.expiresAt < :now")
    List<Reservation> findExpiredPendingReservations(
            @Param("status") Reservation.Status status,
            @Param("now") LocalDateTime now
    );
}
