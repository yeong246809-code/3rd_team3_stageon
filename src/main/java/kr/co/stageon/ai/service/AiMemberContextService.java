package kr.co.stageon.ai.service;

import kr.co.stageon.ai.dto.AiMemberContext;
import kr.co.stageon.ai.dto.AiPerformanceContext;
import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.favorite.domain.PerformanceFavorite;
import kr.co.stageon.favorite.repository.PerformanceFavoriteRepository;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AiMemberContextService {
    private final MemberRepository memberRepository;
    private final PerformanceFavoriteRepository favoriteRepository;
    private final ReservationRepository reservationRepository;

    public AiMemberContextService(
            MemberRepository memberRepository,
            PerformanceFavoriteRepository favoriteRepository,
            ReservationRepository reservationRepository
    ) {
        this.memberRepository = memberRepository;
        this.favoriteRepository = favoriteRepository;
        this.reservationRepository = reservationRepository;
    }

    public Optional<Member> currentMember(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        return memberRepository.findByEmail(authentication.getName())
                .filter(member -> member.getStatus() == Member.Status.ACTIVE);
    }

    public AiMemberContext build(Member member) {
        List<PerformanceFavorite> favorites = favoriteRepository
                .findByMemberIdOrderByCreatedAtDesc(member.getId()).stream().limit(20).toList();
        List<Reservation> reservations = reservationRepository
                .findByMemberIdOrderByCreatedAtDesc(member.getId()).stream()
                .filter(reservation -> reservation.getStatus() == Reservation.Status.RESERVED)
                .limit(20).toList();

        return new AiMemberContext(
                safe(member.getName()),
                ageGroup(member.getBirthDate()),
                distinct(favorites.stream().map(item -> item.getPerformance().getGenre()).toList()),
                distinct(favorites.stream().map(item -> item.getPerformance().getTitle()).toList()),
                distinct(reservations.stream().map(item -> item.getSchedule().getPerformance().getGenre()).toList()),
                distinct(reservations.stream().map(item -> item.getSchedule().getPerformance().getTitle()).toList())
        );
    }

    public List<AiPerformanceContext> personalize(
            List<AiPerformanceContext> candidates,
            AiMemberContext context,
            int limit
    ) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        Set<String> preferredGenres = new LinkedHashSet<>();
        context.favoriteGenres().forEach(genre -> preferredGenres.add(normalize(genre)));
        context.bookedGenres().forEach(genre -> preferredGenres.add(normalize(genre)));
        Set<String> preferredTitles = new LinkedHashSet<>();
        context.favoritePerformances().forEach(title -> preferredTitles.add(normalize(title)));
        context.bookedPerformances().forEach(title -> preferredTitles.add(normalize(title)));

        List<AiPerformanceContext> ranked = new ArrayList<>(candidates);
        ranked.sort(Comparator
                .comparingInt((AiPerformanceContext item) -> score(item, preferredGenres, preferredTitles))
                .reversed()
                .thenComparing(AiPerformanceContext::startDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AiPerformanceContext::id));
        return ranked.stream().limit(Math.max(1, Math.min(limit, 10))).toList();
    }

    private int score(AiPerformanceContext item, Set<String> genres, Set<String> titles) {
        int score = genres.contains(normalize(item.genre())) ? 5 : 0;
        String title = normalize(item.name());
        if (titles.stream().anyMatch(saved -> !saved.isBlank()
                && (title.contains(saved) || saved.contains(title)))) {
            score += 8;
        }
        return score;
    }

    private List<String> distinct(List<String> values) {
        return values.stream().map(this::safe).filter(value -> !value.isBlank())
                .distinct().limit(8).toList();
    }

    private String ageGroup(LocalDate birthDate) {
        if (birthDate == null) return "";
        int age = Math.max(0, Period.between(birthDate, LocalDate.now()).getYears());
        return (age / 10 * 10) + "대";
    }

    private String normalize(String value) {
        return safe(value).toLowerCase(Locale.KOREA).replaceAll("\\s+", "");
    }

    private String safe(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
    }
}
