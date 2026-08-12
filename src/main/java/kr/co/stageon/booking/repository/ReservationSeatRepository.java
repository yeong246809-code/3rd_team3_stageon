package kr.co.stageon.booking.repository;

import kr.co.stageon.booking.domain.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 예매에 포함된 좌석과 결제 당시 가격 DAO입니다. */
public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {
    List<ReservationSeat> findByReservationIdOrderByIdAsc(Long reservationId);
    List<ReservationSeat> findByReservationIdInOrderByIdAsc(List<Long> reservationIds);
}