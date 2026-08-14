package kr.co.stageon.admin.dto;

/** AD 배너 관리 목록 행 DTO입니다. */
public record BannerListItemDto(
        Long id,
        String title,
        String imageUrl,
        String periodStartText,
        String periodEndText,
        int displayOrder,
        boolean active,
        boolean first,
        boolean last,
        String performanceTitle
) {
}