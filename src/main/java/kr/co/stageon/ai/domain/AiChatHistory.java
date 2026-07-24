package kr.co.stageon.ai.domain;

import jakarta.persistence.*;
import kr.co.stageon.member.domain.Member;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** AI 요청과 fallback 여부를 기록하되 운영 좌석·가격 판단에는 사용하지 않습니다. */
@Getter
@Entity
@Table(name = "ai_chat_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "request_type", nullable = false, length = 30)
    private String requestType;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_condition", columnDefinition = "json")
    private String extractedCondition;

    @Column(columnDefinition = "text")
    private String answer;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
