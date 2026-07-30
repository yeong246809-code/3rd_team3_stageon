package kr.co.stageon.queue.repository;

import kr.co.stageon.queue.domain.WaitingQueueHistory;
import kr.co.stageon.queue.domain.WaitingQueueHistory.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Redis 대기열 상태 전이 기록 DAO입니다. */
public interface WaitingQueueHistoryRepository extends JpaRepository<WaitingQueueHistory, Long> {

    List<WaitingQueueHistory> findByScheduleIdAndMemberIdOrderByJoinedAtDesc(Long scheduleId, Long memberId);

    /** 대시보드 - 특정 상태(예: WAITING)의 전체 인원 수 */
    long countByStatus(Status status);

    // Spring Data JPA는 Method Name 매핑을 통해 schedule.id를 탐색하여 조회합니다.
    Optional<WaitingQueueHistory> findByScheduleIdAndQueueTokenHash(Long scheduleId, String queueTokenHash);

    // q.scheduleId 가 아니라 q.schedule.id 로 해야 빨간 줄이 안 생깁니다!
    @Query("SELECT COUNT(q) FROM WaitingQueueHistory q " +
            "WHERE q.schedule.id = :scheduleId " +
            "AND q.status = :status " +
            "AND q.joinedAt < :myJoinedAt")
    int countWaitingAhead(@Param("scheduleId") Long scheduleId,
                          @Param("status") Status status,
                          @Param("myJoinedAt") LocalDateTime myJoinedAt);

    // 특정 회차의 상태별(WAITING 등) 전체 인원 수 조회
    int countByScheduleIdAndStatus(Long scheduleId, Status status);

    //한 번에 10명씩 입장 처리하는 네이티브 쿼리
    @Modifying
    @Query(value = "UPDATE waiting_queue_history SET status = 'ENTERED', entered_at = NOW() " +
            "WHERE schedule_id = :scheduleId AND status = 'WAITING'" +
            "ORDER BY joined_at ASC LIMIT :limit", nativeQuery = true)
    int admitFrontUsers(@Param("scheduleId") Long scheduleId, @Param("limit") int limit);
}