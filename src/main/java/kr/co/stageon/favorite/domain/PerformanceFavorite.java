package kr.co.stageon.favorite.domain;

import jakarta.persistence.*;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.performance.domain.Performance;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 공연 찜 Entity
 *
 * 회원이 특정 공연을 찜한 정보를 저장한다.
 */
@Entity
@Table(
        name = "performance_favorites",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_favorite_member_performance",
                        columnNames = {"member_id", "performance_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PerformanceFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 찜한 회원
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 찜한 공연
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    /**
     * 찜 등록 일시
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Entity 저장 직전에 찜 시간을 자동으로 입력한다.
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}