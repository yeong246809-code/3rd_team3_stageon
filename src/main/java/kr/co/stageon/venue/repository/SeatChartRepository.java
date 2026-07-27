package kr.co.stageon.venue.repository;

import kr.co.stageon.venue.domain.SeatChart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 홀별 좌석도와 Seats.io 차트 식별자 DAO입니다. */
public interface SeatChartRepository extends JpaRepository<SeatChart, Long> {
    List<SeatChart> findByVenueHallIdOrderByVersionDesc(Long venueHallId);
    Optional<SeatChart> findFirstByVenueHallIdAndActiveTrueOrderByVersionDesc(Long venueHallId);
    Optional<SeatChart> findBySeatsioChartKey(String seatsioChartKey);
}
