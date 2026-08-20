package kr.co.stageon.favorite.service;

import kr.co.stageon.favorite.domain.PerformanceFavorite;
import kr.co.stageon.favorite.repository.PerformanceFavoriteRepository;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 공연 찜 기능을 처리하는 Service
 *
 * 찜 등록 / 찜 해제 / 찜 여부 확인 /
 * 회원별 찜 목록 조회 기능을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceFavoriteService {

    private final PerformanceFavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final PerformanceRepository performanceRepository;

    /**
     * 공연 찜하기
     */
    @Transactional
    public void addFavorite(Long memberId, Long performanceId) {

        // 이미 찜한 공연이면 중복 저장하지 않는다.
        if (favoriteRepository.existsByMemberIdAndPerformanceId(
                memberId,
                performanceId
        )) {
            return;
        }

        // 찜할 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 회원입니다.")
                );

        // 찜할 공연 조회
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 공연입니다.")
                );

        // 찜 Entity 생성
        PerformanceFavorite favorite = PerformanceFavorite.builder()
                .member(member)
                .performance(performance)
                .build();

        // DB 저장
        favoriteRepository.save(favorite);
    }

    /**
     * 공연 찜 해제하기
     */
    @Transactional
    public void removeFavorite(Long memberId, Long performanceId) {

        PerformanceFavorite favorite =
                favoriteRepository
                        .findByMemberIdAndPerformanceId(
                                memberId,
                                performanceId
                        )
                        .orElse(null);

        // 찜 정보가 있을 때만 삭제한다.
        if (favorite != null) {
            favoriteRepository.delete(favorite);
        }
    }

    /**
     * 특정 공연의 찜 여부 확인
     */
    public boolean isFavorite(Long memberId, Long performanceId) {

        return favoriteRepository
                .existsByMemberIdAndPerformanceId(
                        memberId,
                        performanceId
                );
    }

    /**
     * 회원이 찜한 공연 목록 조회
     */
    public List<PerformanceFavorite> getFavoriteList(Long memberId) {

        return favoriteRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId);
    }
}