package kr.co.stageon.booking.repository;

import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.performance.domain.Performance;
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
}
