package kr.co.stageon.home.dto;

import java.util.List;

/** 홈에서 탭 하나에 표시할 장르별 최근 7일 예매 랭킹입니다. */
public record HomeGenreRankingView(
        String genre,
        String label,
        List<HomeRankingView> rankings
) {
    public boolean hasRankings() {
        return rankings != null && !rankings.isEmpty();
    }
}
