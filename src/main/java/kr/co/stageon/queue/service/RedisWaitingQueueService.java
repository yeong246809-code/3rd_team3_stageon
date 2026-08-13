package kr.co.stageon.queue.service;

import kr.co.stageon.queue.config.WaitingQueueProperties;
import kr.co.stageon.queue.domain.WaitingQueueHistory;
import kr.co.stageon.queue.dto.QueueInfoResponse;
import kr.co.stageon.queue.repository.WaitingQueueHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Log4j2
@Service
@RequiredArgsConstructor
public class RedisWaitingQueueService {

    private static final String ACTIVE_SCHEDULES_KEY = "queue:active-schedules";
    private static final int PROCESS_RATE_PER_MINUTE = 300;

    private static final DefaultRedisScript<Long> REGISTER_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[3]) == 1 then
                return 0
            end
            local sequence = redis.call('INCR', KEYS[2])
            redis.call('ZADD', KEYS[1], sequence, ARGV[1])
            redis.call('SET', KEYS[3], ARGV[1], 'PX', ARGV[3])
            redis.call('HSET', KEYS[4], 'memberId', ARGV[2], 'status', 'WAITING')
            redis.call('PEXPIRE', KEYS[4], ARGV[3])
            redis.call('PEXPIRE', KEYS[1], ARGV[3])
            redis.call('PEXPIRE', KEYS[2], ARGV[3])
            return sequence
            """, Long.class);

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> ADMIT_SCRIPT = new DefaultRedisScript<>("""
            local tokens = redis.call('ZRANGE', KEYS[1], 0, tonumber(ARGV[1]) - 1)
            local admitted = {}
            for _, token in ipairs(tokens) do
                local ticketKey = ARGV[2] .. token
                local memberId = redis.call('HGET', ticketKey, 'memberId')
                if memberId then
                    redis.call('ZREM', KEYS[1], token)
                    redis.call('SET', ARGV[3] .. token, memberId, 'PX', ARGV[5])
                    redis.call('HSET', ticketKey, 'status', 'ENTERED')
                    redis.call('PEXPIRE', ticketKey, ARGV[5])
                    redis.call('PEXPIRE', ARGV[4] .. memberId, ARGV[5])
                    table.insert(admitted, token)
                else
                    redis.call('ZREM', KEYS[1], token)
                end
            end
            return admitted
            """, List.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;
    private final WaitingQueueHistoryRepository historyRepository;
    private final QueueTokenService queueTokenService;
    private final WaitingQueueProperties properties;

    public boolean register(Long scheduleId, Long memberId, String rawToken) {
        String tokenHash = queueTokenService.hash(rawToken);
        Long result = redisTemplate.execute(
                REGISTER_SCRIPT,
                List.of(
                        waitingKey(scheduleId),
                        sequenceKey(scheduleId),
                        memberKey(scheduleId, memberId),
                        ticketKey(scheduleId, tokenHash)
                ),
                tokenHash,
                memberId.toString(),
                Long.toString(properties.getWaitingTtl().toMillis())
        );

        if (result != null && result > 0) {
            redisTemplate.opsForSet().add(ACTIVE_SCHEDULES_KEY, scheduleId.toString());
            return true;
        }
        return false;
    }

    public boolean isActiveToken(Long scheduleId, Long memberId, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        String tokenHash = queueTokenService.hash(rawToken);
        String waitingToken = redisTemplate.opsForValue().get(memberKey(scheduleId, memberId));
        if (tokenHash.equals(waitingToken)
                && redisTemplate.opsForZSet().score(waitingKey(scheduleId), tokenHash) != null) {
            return true;
        }
        String admittedMember = redisTemplate.opsForValue().get(admissionKey(scheduleId, tokenHash));
        return memberId.toString().equals(admittedMember);
    }

    public QueueInfoResponse getQueueInfo(
            Long performanceId,
            Long scheduleId,
            Long memberId,
            String rawToken
    ) {
        if (rawToken == null || rawToken.isBlank()) {
            return QueueInfoResponse.ofNonWaiting(performanceId, scheduleId, "EXPIRED");
        }

        String tokenHash = queueTokenService.hash(rawToken);
        String admittedMember = redisTemplate.opsForValue().get(admissionKey(scheduleId, tokenHash));
        if (memberId.toString().equals(admittedMember)) {
            return QueueInfoResponse.ofNonWaiting(performanceId, scheduleId, "ENTERED");
        }

        Object ticketMember = redisTemplate.opsForHash()
                .get(ticketKey(scheduleId, tokenHash), "memberId");
        if (!memberId.toString().equals(ticketMember)) {
            return QueueInfoResponse.ofNonWaiting(performanceId, scheduleId, "EXPIRED");
        }

        Long rank = redisTemplate.opsForZSet().rank(waitingKey(scheduleId), tokenHash);
        if (rank == null) {
            return QueueInfoResponse.ofNonWaiting(performanceId, scheduleId, "EXPIRED");
        }

        redisTemplate.opsForSet().add(ACTIVE_SCHEDULES_KEY, scheduleId.toString());

        int waitingOrder = Math.toIntExact(rank);
        int estimatedMinutes = Math.max(
                1,
                (int) Math.ceil((double) (waitingOrder + 1) / PROCESS_RATE_PER_MINUTE)
        );
        Long totalWaiting = redisTemplate.opsForZSet().zCard(waitingKey(scheduleId));
        int total = totalWaiting == null ? 0 : Math.toIntExact(totalWaiting);
        int progressPercentage = total <= 1
                ? 95
                : Math.max(5, Math.min(95, (int) (((double) (total - waitingOrder) / total) * 100)));

        return QueueInfoResponse.builder()
                .performanceId(performanceId)
                .scheduleId(scheduleId)
                .status("WAITING")
                .waitingOrder(waitingOrder)
                .estimatedMinutes(estimatedMinutes)
                .progressPercentage(progressPercentage)
                .build();
    }

    public boolean hasValidAdmission(Long scheduleId, Long memberId, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        String admittedMember = redisTemplate.opsForValue().get(
                admissionKey(scheduleId, queueTokenService.hash(rawToken))
        );
        return memberId.toString().equals(admittedMember);
    }

    @Scheduled(fixedDelayString = "${stageon.queue.process-interval-ms:2000}")
    @Transactional
    public void processQueueAutomatically() {
        RLock schedulerLock = redissonClient.getLock("stageon:queue:admission-scheduler");
        if (!schedulerLock.tryLock()) {
            return;
        }

        try {
            processActiveSchedules();
        } finally {
            if (schedulerLock.isHeldByCurrentThread()) {
                schedulerLock.unlock();
            }
        }
    }

    private void processActiveSchedules() {
        Set<String> activeScheduleIds = redisTemplate.opsForSet().members(ACTIVE_SCHEDULES_KEY);
        if (activeScheduleIds == null || activeScheduleIds.isEmpty()) {
            return;
        }

        for (String rawScheduleId : activeScheduleIds) {
            try {
                admitForSchedule(Long.parseLong(rawScheduleId));
            } catch (RuntimeException exception) {
                log.error("대기열 자동 입장 처리 실패. scheduleId={}", rawScheduleId, exception);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void admitForSchedule(Long scheduleId) {
        List<String> admittedTokenHashes = redisTemplate.execute(
                ADMIT_SCRIPT,
                List.of(waitingKey(scheduleId)),
                Integer.toString(properties.getAdmitBatchSize()),
                ticketPrefix(scheduleId),
                admissionPrefix(scheduleId),
                memberPrefix(scheduleId),
                Long.toString(properties.getAdmissionTtl().toMillis())
        );

        if (admittedTokenHashes != null && !admittedTokenHashes.isEmpty()) {
            LocalDateTime enteredAt = LocalDateTime.now();
            historyRepository.findByQueueTokenHashIn(admittedTokenHashes)
                    .forEach(history -> history.markEntered(enteredAt));
            log.info("대기열 자동 입장 완료. scheduleId={}, count={}", scheduleId, admittedTokenHashes.size());
        }

        Long remaining = redisTemplate.opsForZSet().zCard(waitingKey(scheduleId));
        if (remaining == null || remaining == 0) {
            redisTemplate.opsForSet().remove(ACTIVE_SCHEDULES_KEY, scheduleId.toString());
        }
    }

    @Scheduled(fixedDelayString = "${stageon.queue.expiration-scan-interval-ms:60000}")
    @Transactional
    public void expireStaleHistory() {
        LocalDateTime now = LocalDateTime.now();
        int waitingExpired = historyRepository.expireWaitingBefore(
                WaitingQueueHistory.Status.WAITING,
                WaitingQueueHistory.Status.EXPIRED,
                now.minus(properties.getWaitingTtl()),
                now
        );
        int admissionExpired = historyRepository.expireEnteredBefore(
                WaitingQueueHistory.Status.ENTERED,
                WaitingQueueHistory.Status.EXPIRED,
                now.minus(properties.getAdmissionTtl()),
                now
        );
        if (waitingExpired + admissionExpired > 0) {
            log.info("대기열 만료 이력 정리 완료. waiting={}, admission={}", waitingExpired, admissionExpired);
        }
    }

    private String tag(Long scheduleId) {
        return "queue:{" + scheduleId + "}:";
    }

    private String waitingKey(Long scheduleId) {
        return tag(scheduleId) + "waiting";
    }

    private String sequenceKey(Long scheduleId) {
        return tag(scheduleId) + "sequence";
    }

    private String memberKey(Long scheduleId, Long memberId) {
        return memberPrefix(scheduleId) + memberId;
    }

    private String memberPrefix(Long scheduleId) {
        return tag(scheduleId) + "member:";
    }

    private String ticketPrefix(Long scheduleId) {
        return tag(scheduleId) + "ticket:";
    }

    private String ticketKey(Long scheduleId, String tokenHash) {
        return ticketPrefix(scheduleId) + tokenHash;
    }

    private String admissionPrefix(Long scheduleId) {
        return tag(scheduleId) + "admission:";
    }

    private String admissionKey(Long scheduleId, String tokenHash) {
        return admissionPrefix(scheduleId) + tokenHash;
    }
}
