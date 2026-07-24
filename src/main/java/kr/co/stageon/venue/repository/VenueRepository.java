package kr.co.stageon.venue.repository;

import kr.co.stageon.venue.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 공연장 기본 정보 DAO입니다. */
public interface VenueRepository extends JpaRepository<Venue, Long> {
    List<Venue> findByRegionOrderByNameAsc(String region);
}
