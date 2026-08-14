package kr.co.stageon.home.dto;

import java.util.List;

/** 홈 화면의 각 데이터 영역을 한 번에 전달하는 뷰 모델입니다. */
public record HomePageView(
        List<HomeBannerView> banners,
        List<HomeTicketOpenView> ticketOpenings,
        List<HomeGenreRankingView> genreRankings,
        HomePerformanceView featuredPerformance
) {
    public boolean hasBanners() {
        return banners != null && !banners.isEmpty();
    }

    public boolean hasTicketOpenings() {
        return ticketOpenings != null && !ticketOpenings.isEmpty();
    }

    public boolean hasRankings() {
        return genreRankings != null && genreRankings.stream().anyMatch(HomeGenreRankingView::hasRankings);
    }

    public boolean hasFeaturedPerformance() {
        return featuredPerformance != null;
    }
}