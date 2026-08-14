package kr.co.stageon.home.dto;

import kr.co.stageon.banner.domain.Banner;
import kr.co.stageon.performance.support.PerformanceGenres;

/** 홈 화면 히어로 슬라이더에 노출되는 관리자 등록 배너 읽기 모델입니다. */
public record HomeBannerView(
        Long id,
        String title,
        String description,
        String imageUrl,
        String fallbackImageUrl,
        String periodText,
        String badgeText,
        String button1Text,
        String button1Url,
        String button2Text,
        String button2Url
) {
    public static HomeBannerView from(Banner banner) {
        String period = joinPeriod(banner.getPeriodStartText(), banner.getPeriodEndText());
        String fallback = banner.getPerformance() != null
                ? PerformanceGenres.defaultPosterFor(banner.getPerformance().getGenre())
                : null;

        return new HomeBannerView(
                banner.getId(),
                banner.getTitle(),
                banner.getDescription(),
                banner.getImageUrl(),
                fallback,
                period,
                banner.getBadgeText(),
                banner.getButton1Text(),
                resolveUrl(banner.getButton1Url(), banner, "/schedules"),
                banner.getButton2Text(),
                resolveUrl(banner.getButton2Url(), banner, "")
        );
    }

    private static String resolveUrl(String explicitUrl, Banner banner, String performanceSuffix) {
        if (explicitUrl != null && !explicitUrl.isBlank()) {
            return explicitUrl;
        }
        if (banner.getPerformance() != null) {
            return "/performances/" + banner.getPerformance().getId() + performanceSuffix;
        }
        if (banner.getLinkUrl() != null && !banner.getLinkUrl().isBlank()) {
            return banner.getLinkUrl();
        }
        return "/performances";
    }

    private static String joinPeriod(String start, String end) {
        if ((start == null || start.isBlank()) && (end == null || end.isBlank())) {
            return "";
        }
        if (end == null || end.isBlank()) {
            return start;
        }
        if (start == null || start.isBlank()) {
            return end;
        }
        return start + " ~ " + end;
    }
}