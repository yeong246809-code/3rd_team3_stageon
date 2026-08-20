package kr.co.stageon.admin.dto;

import java.time.LocalDate;

/** AD "버전 관리" 목록 행 DTO입니다. */
public record VersionListItemDto(
        Long id,
        String version,
        String description,
        String author,
        LocalDate releasedAt
) {
}