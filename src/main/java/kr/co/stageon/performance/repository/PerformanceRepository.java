package kr.co.stageon.performance.repository;

import kr.co.stageon.performance.domain.Performance;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 공연 검색과 관리자 공연 관리를 위한 DAO입니다. */
public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    Optional<Performance> findByKopisId(String kopisId);
    List<Performance> findTop12ByOrderByStartDateAsc();
    List<Performance> findByTitleContainingIgnoreCaseOrderByStartDateAsc(String keyword);
    List<Performance> findByGenreOrderByStartDateAsc(String genre);

    /** 동일 홀 + 기간이 겹치는 공연을 조회합니다(공연장 중복 예약 방지 검증용). */
    List<Performance> findByVenueHallIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long venueHallId, LocalDate endDate, LocalDate startDate);

    /** 공개 중이고 아직 종료되지 않은 공연을 검색어와 장르 조건으로 조회합니다. */
    @Query("""
            SELECT p
            FROM Performance p
            WHERE p.draft = false
              AND p.status IN :statuses
              AND p.endDate >= :today
              AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:genre IS NULL OR p.genre = :genre)
            ORDER BY p.startDate ASC, p.id ASC
            """)
    List<Performance> findPublished(
            @Param("keyword") String keyword,
            @Param("genre") String genre,
            @Param("statuses") List<Performance.Status> statuses,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    /** 홈에 노출할 수 있는 공개·진행 예정 공연만 시작일 순으로 조회합니다. */
    @Query("""
            SELECT p
            FROM Performance p
            WHERE p.draft = false
              AND p.status IN :statuses
              AND p.endDate >= :today
            ORDER BY p.startDate ASC, p.id ASC
            """)
    List<Performance> findPublishedForHome(@Param("statuses") List<Performance.Status> statuses,
                                           @Param("today") LocalDate today,
                                           Pageable pageable);

    /** 안개 특집 영역에 관리자가 지정한 공개 공연 한 건을 조회합니다. */
    @Query("""
            SELECT p
            FROM Performance p
            WHERE p.id = :performanceId
              AND p.draft = false
              AND p.status IN :statuses
              AND p.endDate >= :today
            """)
    Optional<Performance> findPublishedByIdForHome(
            @Param("performanceId") Long performanceId,
            @Param("statuses") List<Performance.Status> statuses,
            @Param("today") LocalDate today
    );
}
