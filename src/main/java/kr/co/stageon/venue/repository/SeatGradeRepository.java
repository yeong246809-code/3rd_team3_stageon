package kr.co.stageon.venue.repository;

import kr.co.stageon.venue.domain.SeatGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 공연장별 좌석 등급 DAO입니다. */
public interface SeatGradeRepository extends JpaRepository<SeatGrade, Long> {
    List<SeatGrade> findByVenueIdOrderBySortOrderAsc(Long venueId);
}
