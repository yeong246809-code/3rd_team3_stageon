package kr.co.stageon.booking.repository;

import jakarta.persistence.LockModeType;
import kr.co.stageon.booking.domain.ScheduleSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** 회차 좌석 재고와 선점 상태 DAO입니다. */
public interface ScheduleSeatRepository extends JpaRepository<ScheduleSeat, Long> {
    List<ScheduleSeat> findByScheduleIdOrderBySeatSectionNameAscSeatRowLabelAscSeatSeatNumberAsc(Long scheduleId);

    /** 일정·회차 관리 화면의 잔여석(AVAILABLE 개수) 계산용입니다. */
    long countByScheduleIdAndStatus(Long scheduleId, ScheduleSeat.Status status);

    /**
     * 선점 시 동일 좌석을 직렬화하기 위한 비관적 잠금 조회입니다.
     * 실제 선점 Service는 이 조회 후 AVAILABLE 상태를 다시 검증해야 합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ss from ScheduleSeat ss where ss.id = :id")
    Optional<ScheduleSeat> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT ss FROM ScheduleSeat ss " +
            "JOIN FETCH ss.seat s " +
            "JOIN FETCH s.seatGrade g " +
            "WHERE ss.schedule.id = :scheduleId " +
            "ORDER BY s.sectionName ASC, s.rowLabel ASC, CAST(s.seatNumber AS int) ASC")
    List<ScheduleSeat> findWithSeatInfoByScheduleId(@Param("scheduleId") Long scheduleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ScheduleSeat s set s.status = 'AVAILABLE' where s.id in :seatIds")
    void bulkReleaseSeats(@Param("seatIds") List<Long> seatIds);

    /** AD08 좌석 완전 삭제 판단용 - 이 물리 좌석(seat_id)이 다른 회차에도 등록되어 있는지 확인합니다. */
    boolean existsBySeatId(Long seatId);
}