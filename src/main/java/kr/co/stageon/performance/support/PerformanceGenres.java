package kr.co.stageon.performance.support;

import java.util.List;

/** 화면과 조회 조건에서 사용하는 공연 장르명과 기본 포스터를 한곳에서 관리합니다. */
public final class PerformanceGenres {

    public record Option(String value, String label, String defaultPosterUrl) {
    }

    private static final List<Option> OPTIONS = List.of(
            new Option("뮤지컬", "뮤지컬", "/images/posters/default-musical.webp"),
            new Option("연극", "연극", "/images/posters/default-play.webp"),
            new Option("콘서트", "콘서트", "/images/posters/default-concert.webp"),
            new Option("클래식/무용", "클래식·무용", "/images/posters/default-classic-dance.webp"),
            new Option("행사", "전시·행사", "/images/posters/default-event.webp")
    );

    private PerformanceGenres() {
    }

    public static List<Option> options() {
        return OPTIONS;
    }

    public static String labelFor(String genre) {
        return find(genre).map(Option::label).orElse(genre == null ? "기타" : genre);
    }

    public static String defaultPosterFor(String genre) {
        return find(genre)
                .map(Option::defaultPosterUrl)
                .orElse("/images/posters/default-performance.webp");
    }

    private static java.util.Optional<Option> find(String genre) {
        if (genre == null) {
            return java.util.Optional.empty();
        }
        return OPTIONS.stream().filter(option -> option.value().equals(genre.trim())).findFirst();
    }
}
