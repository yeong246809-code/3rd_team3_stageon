package kr.co.stageon.ai.repository;

import kr.co.stageon.ai.domain.AiChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** AI 요청·응답 및 fallback 이력 DAO입니다. */
public interface AiChatHistoryRepository extends JpaRepository<AiChatHistory, Long> {
    List<AiChatHistory> findTop20ByMemberIdOrderByCreatedAtDesc(Long memberId);
}
