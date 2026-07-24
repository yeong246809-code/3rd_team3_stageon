package kr.co.stageon.booking.repository;

import kr.co.stageon.booking.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 예매 내역과 예매번호 조회 DAO입니다. */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByBookingNumber(String bookingNumber);
    List<Reservation> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<Reservation> findByStatusOrderByCreatedAtDesc(Reservation.Status status);
}
