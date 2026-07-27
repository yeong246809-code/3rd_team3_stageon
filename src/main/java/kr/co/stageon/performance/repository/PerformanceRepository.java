package kr.co.stageon.performance.repository;

import kr.co.stageon.performance.domain.Performance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 공연 검색과 관리자 공연 관리를 위한 DAO입니다. */
public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    Optional<Performance> findByKopisId(String kopisId);
    List<Performance> findTop12ByOrderByStartDateAsc();
    List<Performance> findByTitleContainingIgnoreCaseOrderByStartDateAsc(String keyword);
    List<Performance> findByGenreOrderByStartDateAsc(String genre);
}
