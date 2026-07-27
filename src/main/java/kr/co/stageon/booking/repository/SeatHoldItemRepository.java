package kr.co.stageon.booking.repository;

import kr.co.stageon.booking.domain.SeatHoldItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 좌석 선점에 포함된 회차 좌석 DAO입니다. */
public interface SeatHoldItemRepository extends JpaRepository<SeatHoldItem, Long> {
    List<SeatHoldItem> findBySeatHoldIdOrderByIdAsc(Long seatHoldId);
}
