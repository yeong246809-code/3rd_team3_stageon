package kr.co.stageon.admin.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자 활동(생성/수정/삭제 등) 이력을 기록하는 엔티티.
 * AdminActivityLogAspect(@AdminLoggable AOP)를 통해서만 생성됩니다.
 */
@Entity
@Table(name = "admin_activity_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "admin_email", length = 190)
    private String adminEmail;

    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;

    @Column(name = "target_entity", nullable = false, length = 50)
    private String targetEntity;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private AdminActivityLog(Long adminId, String adminEmail, String actionType,
                             String targetEntity, Long targetId, String description) {
        this.adminId = adminId;
        this.adminEmail = adminEmail;
        this.actionType = actionType;
        this.targetEntity = targetEntity;
        this.targetId = targetId;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public static AdminActivityLog create(Long adminId, String adminEmail, String actionType,
                                          String targetEntity, Long targetId, String description) {
        return new AdminActivityLog(adminId, adminEmail, actionType, targetEntity, targetId, description);
    }
}