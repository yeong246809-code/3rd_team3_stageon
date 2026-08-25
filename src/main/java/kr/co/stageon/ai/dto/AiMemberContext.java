package kr.co.stageon.ai.dto;

import java.util.List;

/** AI에 전달해도 되는 로그인 회원의 최소 추천 정보입니다. */
public record AiMemberContext(
        String displayName,
        String ageGroup,
        List<String> favoriteGenres,
        List<String> favoritePerformances,
        List<String> bookedGenres,
        List<String> bookedPerformances
) {
    public static AiMemberContext anonymous() {
        return new AiMemberContext("", "", List.of(), List.of(), List.of(), List.of());
    }

    public boolean personalized() {
        return !favoriteGenres.isEmpty() || !favoritePerformances.isEmpty()
                || !bookedGenres.isEmpty() || !bookedPerformances.isEmpty();
    }
}
