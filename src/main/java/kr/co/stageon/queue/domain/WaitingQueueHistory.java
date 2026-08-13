package kr.co.stageon.queue.domain;

import jakarta.persistence.*;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Redis 대기열의 핵심 상태 전이를 감사·성능 측정용으로 보존합니다. */
@Getter
@Entity
@Table(name = "waiting_queue_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WaitingQueueHistory {

    public enum Status { WAITING, ENTERED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private PerformanceSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "queue_token_hash", nullable = false, unique = true, length = 64)
    private String queueTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @lombok.Builder
    public WaitingQueueHistory(PerformanceSchedule schedule, Member member, String queueTokenHash, Status status, LocalDateTime joinedAt, LocalDateTime enteredAt) {
        this.schedule = schedule;
        this.member = member;
        this.queueTokenHash = queueTokenHash;
        this.status = status;
        this.joinedAt = joinedAt;
        this.enteredAt = enteredAt;
    }

    public void markEntered(LocalDateTime enteredAt) {
        this.status = Status.ENTERED;
        this.enteredAt = enteredAt;
        this.expiredAt = null;
    }

    public void markExpired(LocalDateTime expiredAt) {
        this.status = Status.EXPIRED;
        this.expiredAt = expiredAt;
    }
}
