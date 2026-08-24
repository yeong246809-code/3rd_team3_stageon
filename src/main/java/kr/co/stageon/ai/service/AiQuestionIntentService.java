package kr.co.stageon.ai.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class AiQuestionIntentService {
    private static final List<String> STAGEON_TERMS = List.of(
            "stageon", "스테이지온", "이 사이트", "우리 사이트", "여기서", "여기에서"
    );
    private static final List<String> BOOKABLE_TERMS = List.of(
            "예매 가능", "예매가능", "예매할 수", "예매되는", "예매 중", "예매중",
            "티켓 구매 가능", "구매 가능한", "지금 예매", "현재 예매"
    );
    private static final List<String> KOPIS_TERMS = List.of("kopis", "코피스");
    private static final List<String> FOLLOW_UP_TERMS = List.of(
            "그중", "그 중", "이중", "이 중", "아까", "방금", "위에서", "추천한",
            "첫 번째", "첫번째", "두 번째", "두번째", "세 번째", "세번째", "1번", "2번", "3번",
            "그 공연", "그거", "더 저렴", "더 싼", "제일 싼", "가장 싼", "제일 저렴", "가장 저렴",
            "최저가", "가격", "얼마", "출연진", "배우", "장소", "어디서", "언제", "관람연령",
            "아이도", "주말에도", "오늘도", "내일도", "러닝타임", "다른 공연", "다른 건", "다른거"
    );
    private static final List<String> RESET_SEARCH_TERMS = List.of(
            "새로 검색", "새로 찾아", "처음부터", "다시 검색", "다시 찾아",
            "다른 지역", "다른 장르", "새로운 공연"
    );
    private static final List<String> SEARCH_ACTION_TERMS = List.of(
            "추천해줘", "추천해 줘", "공연 찾아", "공연 검색", "공연 알려줘", "공연 알려 줘"
    );
    private static final List<String> GENRE_TERMS = List.of(
            "연극", "뮤지컬", "콘서트", "클래식", "국악", "무용", "발레", "오페라", "마술", "서커스"
    );
    private static final List<String> REGION_TERMS = List.of(
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "경기", "강원",
            "충북", "충남", "전북", "전남", "경북", "경남", "제주"
    );
    private static final List<String> DATE_TERMS = List.of(
            "오늘", "내일", "이번 주", "이번주", "이번 주말", "이번주말", "이번 달", "이번달"
    );

    public boolean requiresStageonBookableData(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String normalized = question.toLowerCase(Locale.KOREA).replaceAll("\\s+", " ").trim();
        boolean mentionsStageon = STAGEON_TERMS.stream().anyMatch(normalized::contains);
        boolean explicitlyKopis = KOPIS_TERMS.stream().anyMatch(normalized::contains);
        boolean asksBookable = BOOKABLE_TERMS.stream().anyMatch(normalized::contains);
        return mentionsStageon || (asksBookable && !explicitlyKopis);
    }

    public boolean isFollowUp(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String normalized = question.toLowerCase(Locale.KOREA).replaceAll("\\s+", " ").trim();
        return FOLLOW_UP_TERMS.stream().anyMatch(normalized::contains);
    }

    public boolean shouldReusePreviousResults(
            String question,
            boolean hasPreviousPerformances,
            String memoryMode
    ) {
        if (!hasPreviousPerformances || question == null || question.isBlank()) {
            return false;
        }
        if (isFollowUp(question)) {
            return true;
        }
        if (!"conversation-first".equalsIgnoreCase(memoryMode)) {
            return false;
        }
        return !startsNewSearch(question);
    }

    private boolean startsNewSearch(String question) {
        String normalized = question.toLowerCase(Locale.KOREA).replaceAll("\\s+", " ").trim();
        if (RESET_SEARCH_TERMS.stream().anyMatch(normalized::contains)) {
            return true;
        }
        if (STAGEON_TERMS.stream().anyMatch(normalized::contains)
                || KOPIS_TERMS.stream().anyMatch(normalized::contains)) {
            return true;
        }
        boolean hasSearchAction = SEARCH_ACTION_TERMS.stream().anyMatch(normalized::contains);
        boolean hasNewCondition = GENRE_TERMS.stream().anyMatch(normalized::contains)
                || REGION_TERMS.stream().anyMatch(normalized::contains)
                || DATE_TERMS.stream().anyMatch(normalized::contains);
        return hasSearchAction || hasNewCondition;
    }
}
