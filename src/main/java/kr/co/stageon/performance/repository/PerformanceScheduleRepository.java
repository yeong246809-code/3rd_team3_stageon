package kr.co.stageon.performance.repository;

import kr.co.stageon.performance.domain.PerformanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 공연 회차 조회와 일정 관리를 위한 DAO입니다. */
public interface PerformanceScheduleRepository extends JpaRepository<PerformanceSchedule, Long> {
    List<PerformanceSchedule> findByPerformanceIdOrderByStartsAtAsc(Long performanceId);
    List<PerformanceSchedule> findByVenueHallIdOrderByStartsAtAsc(Long venueHallId);
}
