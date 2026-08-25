package kr.co.stageon.admin.dto;

import kr.co.stageon.admin.domain.AdminActivityLog;

import java.time.LocalDateTime;

/** 감사 로그 목록 화면 표시용 DTO */
public record AdminActivityLogRowDto(
        Long id,
        String adminEmail,
        String actionType,
        String targetEntity,
        Long targetId,
        String description,
        LocalDateTime createdAt
) {
    public static AdminActivityLogRowDto from(AdminActivityLog log) {
        return new AdminActivityLogRowDto(
                log.getId(), log.getAdminEmail(), log.getActionType(), log.getTargetEntity(),
                log.getTargetId(), log.getDescription(), log.getCreatedAt()
        );
    }
}