package kr.co.stageon.venue.repository;

import kr.co.stageon.venue.domain.VenueHall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 공연시설 내부 홀 DAO입니다. */
public interface VenueHallRepository extends JpaRepository<VenueHall, Long> {
    List<VenueHall> findByVenueIdAndActiveTrueOrderByNameAsc(Long venueId);
    Optional<VenueHall> findByKopisHallId(String kopisHallId);
}
