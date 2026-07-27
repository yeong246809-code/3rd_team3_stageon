package kr.co.stageon.venue.repository;

import kr.co.stageon.venue.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 물리 좌석 배치 DAO입니다. */
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findBySeatChartIdOrderBySectionNameAscRowLabelAscSeatNumberAsc(Long seatChartId);
}
