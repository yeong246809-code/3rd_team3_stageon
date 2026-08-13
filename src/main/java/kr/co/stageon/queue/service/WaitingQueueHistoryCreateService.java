package kr.co.stageon.queue.service;

import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import kr.co.stageon.performance.support.ScheduleSalesPolicy;
import kr.co.stageon.queue.domain.WaitingQueueHistory;
import kr.co.stageon.queue.repository.WaitingQueueHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * [남수아 담당]
 * 회차 선택 화면에서 전달받은 scheduleId와 로그인 회원 정보를 이용해
 * waiting_queue_history 이력과 Redis 실시간 대기열을 함께 등록합니다.
 */
@Service
@RequiredArgsConstructor
public class WaitingQueueHistoryCreateService {

    private final WaitingQueueHistoryRepository waitingQueueHistoryRepository;
    private final PerformanceScheduleRepository performanceScheduleRepository;
    private final MemberRepository memberRepository;
    private final RedisWaitingQueueService redisWaitingQueueService;
    private final QueueTokenService queueTokenService;

    /**
     * 선택한 회차와 로그인 회원을 기준으로 WAITING 상태의 이력을 저장합니다.
     *
     * queueToken 원문은 HttpOnly 쿠키 발급에만 사용하도록 반환하고,
     * DB에는 SHA-256으로 변환한 64자리 해시값만 저장합니다.
     *
     * @param scheduleId 선택한 공연 회차 번호
     * @param memberEmail Spring Security가 보유한 로그인 회원 이메일
     * @return 생성된 이력 번호와 원본 토큰 등 연동에 필요한 값
     */
    @Transactional
    public QueueEntryResult create(
            Long scheduleId,
            String memberEmail,
            String currentQueueToken
    ) {
        if (scheduleId == null) {
            throw new IllegalArgumentException("회차 번호가 필요합니다.");
        }

        String normalizedEmail = memberEmail == null
                ? ""
                : memberEmail.trim().toLowerCase();

        Member member = memberRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "로그인 회원 정보를 찾을 수 없습니다."
                        )
                );

        PerformanceSchedule schedule = performanceScheduleRepository
                .findById(scheduleId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "선택한 공연 회차를 찾을 수 없습니다."
                        )
                );

        validateSalesWindow(schedule);

        if (redisWaitingQueueService.isActiveToken(scheduleId, member.getId(), currentQueueToken)) {
            String currentTokenHash = queueTokenService.hash(currentQueueToken);
            WaitingQueueHistory currentHistory = waitingQueueHistoryRepository
                    .findByScheduleIdAndQueueTokenHash(scheduleId, currentTokenHash)
                    .orElseThrow(() -> new IllegalStateException("대기열 이력을 찾을 수 없습니다."));
            return QueueEntryResult.from(currentHistory, currentQueueToken);
        // 현재 서버 시간 기준으로 예매 가능 여부를 한 번 더 확인합니다.
        // 프론트 화면에서 버튼을 막더라도 사용자가 직접 요청을 보낼 수 있기 때문에
        // 실제 대기열 등록 직전에 서버에서 검증하는 것이 안전합니다.
                LocalDateTime now = LocalDateTime.now();

        // 예매 시작 전이거나 예매 종료 시간이 지난 경우에는 대기열 등록을 막습니다.
        if (!ScheduleSalesPolicy.isBookable(schedule, now)) {
            throw new IllegalStateException("현재 예매 가능한 회차가 아닙니다.");
        }

        // Redis에서 사용할 수 있는 예측 불가능한 원본 토큰을 생성합니다.
        String queueToken = queueTokenService.issue();

        // DDL의 queue_token_hash VARCHAR(64)에 맞춰 SHA-256 16진수 값을 저장합니다.
        String queueTokenHash = queueTokenService.hash(queueToken);
        LocalDateTime joinedAt = LocalDateTime.now();

        WaitingQueueHistory history = WaitingQueueHistory.builder()
                .schedule(schedule)
                .member(member)
                .queueTokenHash(queueTokenHash)
                .status(WaitingQueueHistory.Status.WAITING)
                .joinedAt(joinedAt)
                .build();

        WaitingQueueHistory savedHistory = waitingQueueHistoryRepository.saveAndFlush(history);

        if (!redisWaitingQueueService.register(scheduleId, member.getId(), queueToken)) {
            throw new IllegalStateException("이미 이 회차의 대기열에 참여 중입니다. 기존 대기 화면을 이용해 주세요.");
        }

        return new QueueEntryResult(
                savedHistory.getId(),
                schedule.getPerformance().getId(),
                schedule.getId(),
                member.getId(),
                queueToken,
                queueTokenHash,
                savedHistory.getStatus().name(),
                joinedAt
        );

    private void validateSalesWindow(PerformanceSchedule schedule) {
        LocalDateTime now = LocalDateTime.now();
        if (schedule.getStatus() == PerformanceSchedule.Status.CANCELLED
                || schedule.getStatus() == PerformanceSchedule.Status.CLOSED) {
            throw new IllegalArgumentException("예매할 수 없는 회차입니다.");
        }
        if (now.isBefore(schedule.getSalesOpenAt())) {
            throw new IllegalArgumentException("아직 예매가 시작되지 않은 회차입니다.");
        }
        if (!now.isBefore(schedule.getSalesCloseAt())) {
            throw new IllegalArgumentException("예매가 종료된 회차입니다.");
        }
    }

    public record QueueEntryResult(
            Long historyId,
            Long performanceId,
            Long scheduleId,
            Long memberId,
            String queueToken,
            String queueTokenHash,
            String status,
            LocalDateTime joinedAt
    ) {
        private static QueueEntryResult from(WaitingQueueHistory history, String rawToken) {
            return new QueueEntryResult(
                    history.getId(),
                    history.getSchedule().getPerformance().getId(),
                    history.getSchedule().getId(),
                    history.getMember().getId(),
                    rawToken,
                    history.getQueueTokenHash(),
                    history.getStatus().name(),
                    history.getJoinedAt()
            );
        }
    }
}
