package kr.co.stageon.performance.repository;

import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
            "JOIN FETCH ps.venueHall " +
            "WHERE ps.performance.id = :performanceId " +
            "AND ps.startsAt >= :from AND ps.startsAt < :to " +
            "ORDER BY ps.startsAt ASC")
    List<PerformanceSchedule> findByPerformanceAndMonth(@Param("performanceId") Long performanceId,
                                                        @Param("from") LocalDateTime from,
                                                        @Param("to") LocalDateTime to);

    /**
     * 특정 홀에서 다른 공연이 이미 점유한 회차 목록을 조회합니다(취소된 회차는 제외).
     * 공연장 중복 예약 방지(같은 홀·같은 날짜에는 한 공연만) 검증에 사용됩니다.
     */
    @Query("SELECT ps FROM PerformanceSchedule ps " +
            "WHERE ps.venueHall.id = :venueHallId " +
            "AND ps.performance.id <> :excludePerformanceId " +
            "AND ps.status <> kr.co.stageon.performance.domain.PerformanceSchedule.Status.CANCELLED")
    List<PerformanceSchedule> findOtherPerformanceSchedulesInHall(@Param("venueHallId") Long venueHallId,
                                                                  @Param("excludePerformanceId") Long excludePerformanceId);

    /** 홈 티켓 오픈 영역용으로 공연·공연장 정보를 함께 조회합니다. */
    @Query("""
            SELECT ps
            FROM PerformanceSchedule ps
            JOIN FETCH ps.performance p
            JOIN FETCH ps.venueHall vh
            JOIN FETCH vh.venue v
            WHERE ps.salesOpenAt >= :now
              AND ps.status IN :statuses
              AND p.draft = false
              AND p.status IN :performanceStatuses
            ORDER BY ps.salesOpenAt ASC, ps.id ASC
            """)
    List<PerformanceSchedule> findUpcomingTicketOpenings(
            @Param("now") LocalDateTime now,
            @Param("statuses") List<PerformanceSchedule.Status> statuses,
            @Param("performanceStatuses") List<Performance.Status> performanceStatuses,
            Pageable pageable
    );

    // 1. 오픈 시간이 지났고, 아직 마감 시간이 안 된 예정된(SCHEDULED) 회차를 OPEN으로 변경
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PerformanceSchedule ps SET ps.status = 'OPEN' " +
            "WHERE ps.status = 'SCHEDULED' AND ps.salesOpenAt <= :now AND ps.salesCloseAt > :now")
    int openSchedules(@Param("now") LocalDateTime now);

    // 2. 마감 시간이 지난 회차를 CLOSED로 변경 (예정 상태이거나 오픈 상태였던 것들 모두)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PerformanceSchedule ps SET ps.status = 'CLOSED' " +
            "WHERE ps.status IN ('SCHEDULED', 'OPEN') AND ps.salesCloseAt <= :now")
    int closeSchedules(@Param("now") LocalDateTime now);
}