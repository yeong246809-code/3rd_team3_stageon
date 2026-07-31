package kr.co.stageon.queue.service;

import jakarta.persistence.EntityManager; // 👈 추가됨
import kr.co.stageon.queue.domain.WaitingQueueHistory;
import kr.co.stageon.queue.domain.WaitingQueueHistory.Status;
import kr.co.stageon.queue.dto.QueueInfoResponse;
import kr.co.stageon.queue.repository.WaitingQueueHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisWaitingQueueService {

    private final RedisTemplate<String, String> redisTemplate;
    private final WaitingQueueHistoryRepository historyRepository;

    // 💡 (핵심) JPA 엔티티 매니저 주입 (DB 조작의 핵심 도구)
    private final EntityManager entityManager;

    private static final int PROCESS_RATE_PER_MINUTE = 300;

    // 1. 대기열 줄 서기
    public void joinQueue(Long scheduleId, String queueToken) {
        String waitingKey = "queue:schedule:" + scheduleId + ":waiting";
        long timestamp = System.currentTimeMillis();
        redisTemplate.opsForZSet().addIfAbsent(waitingKey, queueToken, timestamp);
    }

    // 2. 대기열 상태 조회
    public QueueInfoResponse getQueueInfo(Long performanceId, Long scheduleId, String queueToken) {
        String waitingKey = "queue:schedule:" + scheduleId + ":waiting";
        String enteredKey = "queue:schedule:" + scheduleId + ":entered";

        Boolean isEntered = redisTemplate.opsForSet().isMember(enteredKey, queueToken);
        if (Boolean.TRUE.equals(isEntered)) {
            return QueueInfoResponse.ofNonWaiting(performanceId, scheduleId, queueToken, "ENTERED");
        }

        Long rank = redisTemplate.opsForZSet().rank(waitingKey, queueToken);
        if (rank == null) {
            joinQueue(scheduleId, queueToken);
            rank = redisTemplate.opsForZSet().rank(waitingKey, queueToken);
        }

        int waitingOrder = rank.intValue();
        int estimatedMinutes = Math.max(1, (int) Math.ceil((double) waitingOrder / PROCESS_RATE_PER_MINUTE));

        Long totalWaiting = redisTemplate.opsForZSet().zCard(waitingKey);
        int total = totalWaiting != null ? totalWaiting.intValue() : 0;
        int progressPercentage = total > 0
                ? (int) (((double) (total - waitingOrder) / total) * 100)
                : 100;

        return QueueInfoResponse.builder()
                .performanceId(performanceId)
                .scheduleId(scheduleId)
                .queueToken(queueToken)
                .status("WAITING")
                .waitingOrder(waitingOrder)
                .estimatedMinutes(estimatedMinutes)
                .progressPercentage(progressPercentage)
                .build();
    }

    // 3. 스케줄러: 5초마다 10명씩 자동 입장 처리
    @Scheduled(fixedDelay = 5000)
    public void processQueueAutomatically() {
        Long scheduleId = 1L; // 테스트용 회차
        String waitingKey = "queue:schedule:" + scheduleId + ":waiting";
        String enteredKey = "queue:schedule:" + scheduleId + ":entered";

        Set<String> frontUsers = redisTemplate.opsForZSet().range(waitingKey, 0, 9);
        if (frontUsers != null && !frontUsers.isEmpty()) {
            redisTemplate.opsForZSet().remove(waitingKey, frontUsers.toArray());
            redisTemplate.opsForSet().add(enteredKey, frontUsers.toArray(new String[0]));
            System.out.println("🚀 [Redis 문지기] 앞사람 " + frontUsers.size() + "명이 초고속 입장 완료!");
        }
    }

    // 4. 백업 스케줄러: 10초마다 입장자들을 MySQL에 영구 기록
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void backupEnteredUsersToDB() {
        Long scheduleId = 1L;
        String enteredKey = "queue:schedule:" + scheduleId + ":entered";

        // 하나씩 100번 꺼내기
        List<String> enteredTokens = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String token = redisTemplate.opsForSet().pop(enteredKey);
            if (token == null) {
                break;
            }
            enteredTokens.add(token);
        }

        if (!enteredTokens.isEmpty()) {
            List<WaitingQueueHistory> histories = new ArrayList<>();

            // 🚨 (수정됨) new 대신 EntityManager.getReference()를 사용하여 가짜(Proxy) 객체 생성!
            kr.co.stageon.performance.domain.PerformanceSchedule dummySchedule =
                    entityManager.getReference(kr.co.stageon.performance.domain.PerformanceSchedule.class, scheduleId);
            kr.co.stageon.member.domain.Member dummyMember =
                    entityManager.getReference(kr.co.stageon.member.domain.Member.class, 1L); // 테스트용 멤버 ID 1L

            for (String token : enteredTokens) {
                WaitingQueueHistory history = WaitingQueueHistory.builder()
                        .schedule(dummySchedule)
                        .member(dummyMember)
                        .queueTokenHash(token)
                        .status(Status.ENTERED)
                        .enteredAt(java.time.LocalDateTime.now())
                        .build();
                histories.add(history);
            }

            historyRepository.saveAll(histories);
            System.out.println("💾 [데이터 백업 완료] " + enteredTokens.size() + "명의 입장 기록을 MySQL에 안전하게 백업했습니다!");
        }
    }
}