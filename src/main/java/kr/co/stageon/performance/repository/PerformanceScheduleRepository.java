package kr.co.stageon.performance.repository;

import kr.co.stageon.performance.domain.PerformanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 공연 회차 조회와 일정 관리를 위한 DAO입니다. */
public interface PerformanceScheduleRepository extends JpaRepository<PerformanceSchedule, Long> {
    List<PerformanceSchedule> findByPerformanceIdOrderByStartsAtAsc(Long performanceId);
    List<PerformanceSchedule> findByVenueHallIdOrderByStartsAtAsc(Long venueHallId);

    long countByStatus(PerformanceSchedule.Status status);

    /** 대시보드 요약/충돌 검사용으로 공연·공연장홀 정보를 함께 조회합니다. */
    @Query("SELECT ps FROM PerformanceSchedule ps " +
            "JOIN FETCH ps.performance " +
            "JOIN FETCH ps.venueHall " +
            "WHERE ps.status IN :statuses " +
            "ORDER BY ps.startsAt ASC")
    List<PerformanceSchedule> findActiveWithDetails(@Param("statuses") List<PerformanceSchedule.Status> statuses);

    /** 특정 공연의 특정 월 회차 목록을 시작시간 순으로 조회합니다. */
    @Query("SELECT ps FROM PerformanceSchedule ps " +
            "JOIN FETCH ps.performance " +
            "WHERE ps.performance.id = :performanceId " +
            "AND ps.startsAt >= :from AND ps.startsAt < :to " +
            "ORDER BY ps.startsAt ASC")
    List<PerformanceSchedule> findByPerformanceAndMonth(@Param("performanceId") Long performanceId,
                                                        @Param("from") LocalDateTime from,
                                                        @Param("to") LocalDateTime to);
}