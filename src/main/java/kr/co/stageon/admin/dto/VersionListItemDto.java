package kr.co.stageon.admin.dto;

import kr.co.stageon.version.domain.Version;

import java.time.LocalDate;

/** AD "버전 관리" 목록 행 DTO입니다. */
public record VersionListItemDto(
        Long id,
        String version,
        Version.Category category,
        String title,
        String author,
        LocalDate releasedAt
) {
}