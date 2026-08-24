package kr.co.stageon.ai.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiQuestionIntentServiceTest {
    private final AiQuestionIntentService service = new AiQuestionIntentService();

    @Test
    void routesStageonQuestionsToTheStageonDatabase() {
        assertThat(service.requiresStageonBookableData("StageOn에서 볼 수 있는 공연 알려줘"))
                .isTrue();
        assertThat(service.requiresStageonBookableData("여기서 지금 예매 가능한 뮤지컬 있어?"))
                .isTrue();
    }

    @Test
    void routesGenericPerformanceInformationToKopis() {
        assertThat(service.requiresStageonBookableData("서울에서 볼 수 있는 뮤지컬 추천해줘"))
                .isFalse();
    }

    @Test
    void explicitKopisRequestDoesNotUseStageonInventory() {
        assertThat(service.requiresStageonBookableData("KOPIS에서 예매 가능한 공연을 알려줘"))
                .isFalse();
    }

    @Test
    void detectsReferencesToPreviousRecommendations() {
        assertThat(service.isFollowUp("그중 두 번째 공연 가격은 얼마야?"))
                .isTrue();
        assertThat(service.isFollowUp("아까 추천한 것보다 더 저렴한 건?"))
                .isTrue();
        assertThat(service.isFollowUp("서울 뮤지컬을 추천해줘"))
                .isFalse();
    }

    @Test
    void conversationFirstTreatsAmbiguousQuestionsAsFollowUps() {
        assertThat(service.shouldReusePreviousResults(
                "제일 싼 건 뭐야?", true, "conversation-first"))
                .isTrue();
        assertThat(service.shouldReusePreviousResults(
                "평일 공연도 있어?", true, "conversation-first"))
                .isTrue();
    }

    @Test
    void explicitNewConditionsStartANewSearch() {
        assertThat(service.shouldReusePreviousResults(
                "부산 뮤지컬을 추천해줘", true, "conversation-first"))
                .isFalse();
        assertThat(service.shouldReusePreviousResults(
                "KOPIS에서 새로 찾아줘", true, "conversation-first"))
                .isFalse();
    }
}
