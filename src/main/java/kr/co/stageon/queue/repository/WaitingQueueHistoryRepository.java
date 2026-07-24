package kr.co.stageon.queue.repository;

import kr.co.stageon.queue.domain.WaitingQueueHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Redis 대기열 상태 전이 기록 DAO입니다. */
public interface WaitingQueueHistoryRepository extends JpaRepository<WaitingQueueHistory, Long> {
    List<WaitingQueueHistory> findByScheduleIdAndMemberIdOrderByJoinedAtDesc(Long scheduleId, Long memberId);
}
