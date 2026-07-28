package kr.co.stageon.booking.repository;

import kr.co.stageon.booking.domain.Reservation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 예매 내역과 예매번호 조회 DAO입니다. */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
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
}