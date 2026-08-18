package kr.co.stageon.booking.repository;

import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.domain.SeatHoldItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 좌석 선점에 포함된 회차 좌석 DAO입니다. */
public interface SeatHoldItemRepository extends JpaRepository<SeatHoldItem, Long> {
    List<SeatHoldItem> findBySeatHoldIdOrderByIdAsc(Long seatHoldId);

    List<SeatHoldItem> findBySeatHoldIn(List<SeatHold> seatHolds);

    List<SeatHoldItem> findBySeatHoldId(Long seatHoldId);

    /** AD08 좌석 삭제 전 검증용 - 해당 회차 좌석이 과거에 한 번이라도 선점된 이력이 있는지 확인합니다. */
    long countByScheduleSeatId(Long scheduleSeatId);

    /** 한 회원이 동일 공연에서 현재 선점 중인 실제 좌석 수입니다. */
    @Query("""
            SELECT COUNT(item.id)
            FROM SeatHoldItem item
            WHERE item.seatHold.member.id = :memberId
              AND item.seatHold.schedule.performance.id = :performanceId
              AND item.seatHold.status = :status
            """)
    long countActiveSeatsByMemberAndPerformance(
            @Param("memberId") Long memberId,
            @Param("performanceId") Long performanceId,
            @Param("status") SeatHold.Status status
    );
}
