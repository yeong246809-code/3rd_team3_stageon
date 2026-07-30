package kr.co.stageon.queue.service;

import kr.co.stageon.queue.domain.WaitingQueueHistory;
import kr.co.stageon.queue.domain.WaitingQueueHistory.Status;
import kr.co.stageon.queue.dto.QueueInfoResponse;
import kr.co.stageon.queue.repository.WaitingQueueHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WaitingQueueService {

    private final WaitingQueueHistoryRepository queueRepository;

    // 1분에 빠져나가는 평균 대기 인원 (예: 1분에 300명 처리 기준)
    private static final int PROCESS_RATE_PER_MINUTE = 300;

    public QueueInfoResponse getQueueInfo(Long performanceId, Long scheduleId, String queueToken) {
        // 1. DB에서 내 대기열 이력 조회
        WaitingQueueHistory myQueue = queueRepository.findByScheduleIdAndQueueTokenHash(scheduleId, queueToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 대기열 토큰입니다."));

        // 2. String이 아닌 Enum 타입(Status)으로 안전하게 변수 선언
        Status status = myQueue.getStatus();

        // 3. 이미 입장 허용(ENTERED)되었거나 만료(EXPIRED)된 경우 순위 계산 없이 반환
        // (DTO로 넘어갈 때는 문자열이 필요하므로 status.name()으로 변환)
        if (status != Status.WAITING) {
            return QueueInfoResponse.ofNonWaiting(performanceId, scheduleId, queueToken, status.name());
        }

        // 4. 'WAITING' 상태인 경우: 내 앞 대기 인원 계산 (Enum 파라미터 전달)
        int waitingOrder = queueRepository.countWaitingAhead(scheduleId, Status.WAITING, myQueue.getJoinedAt());

        // 5. 예상 대기 시간 계산 (분 단위, 최소 1분 보장)
        int estimatedMinutes = Math.max(1, (int) Math.ceil((double) waitingOrder / PROCESS_RATE_PER_MINUTE));

        // 6. 진행률 계산 (전체 대기자 중 내 위치를 기반으로 0~100% 산정)
        int totalWaiting = queueRepository.countByScheduleIdAndStatus(scheduleId, Status.WAITING);
        int progressPercentage = totalWaiting > 0
                ? (int) (((double) (totalWaiting - waitingOrder) / totalWaiting) * 100)
                : 100;

        // 7. DTO 변환 후 반환
        return QueueInfoResponse.builder()
                .performanceId(performanceId)
                .scheduleId(scheduleId)
                .queueToken(queueToken)
                .status(status.name()) // Enum -> String 변환
                .waitingOrder(waitingOrder)
                .estimatedMinutes(estimatedMinutes)
                .progressPercentage(progressPercentage)
                .build();
    }

    // 5초마다 딱 한 번 DB에 접근해서 10명을 통째로 입장 처리합니다.
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processQueueAutomatically() {
        Long scheduleId = 1L; // 테스트용 1번 회차 고정
        int admittedCount = queueRepository.admitFrontUsers(scheduleId, 10);
    }
}