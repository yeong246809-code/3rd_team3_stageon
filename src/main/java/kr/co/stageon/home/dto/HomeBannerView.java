package kr.co.stageon.home.dto;

import kr.co.stageon.banner.domain.Banner;
import kr.co.stageon.performance.support.PerformanceGenres;

import java.time.format.DateTimeFormatter;

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
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static HomeBannerView from(Banner banner) {
        String period = joinPeriod(banner.getPeriodStart(), banner.getPeriodEnd());
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

    private static String joinPeriod(java.time.LocalDate start, java.time.LocalDate end) {
        if (start == null && end == null) {
            return "";
        }
        if (end == null) {
            return DATE_FORMATTER.format(start);
        }
        if (start == null) {
            return DATE_FORMATTER.format(end);
        }
        return DATE_FORMATTER.format(start) + " ~ " + DATE_FORMATTER.format(end);
    }
}