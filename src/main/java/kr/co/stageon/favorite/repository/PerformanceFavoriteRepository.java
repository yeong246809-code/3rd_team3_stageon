package kr.co.stageon.favorite.repository;

import kr.co.stageon.favorite.domain.PerformanceFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 공연 찜 Repository
 *
 * 찜 여부 확인, 회원별 찜 목록 조회,
 * 찜 정보 삭제 등에 사용한다.
 */
public interface PerformanceFavoriteRepository
        extends JpaRepository<PerformanceFavorite, Long> {

    /**
     * 특정 회원이 특정 공연을 찜했는지 확인
     */
    boolean existsByMemberIdAndPerformanceId(
            Long memberId,
            Long performanceId
    );

    /**
     * 특정 회원 + 공연의 찜 정보 조회
     *
     * 나중에 찜 해제할 때 사용한다.
     */
    Optional<PerformanceFavorite> findByMemberIdAndPerformanceId(
            Long memberId,
            Long performanceId
    );

    /**
     * 특정 회원이 찜한 공연 목록 조회
     *
     * 최근 찜한 공연부터 보여준다.
     */
    List<PerformanceFavorite> findByMemberIdOrderByCreatedAtDesc(
            Long memberId
    );
}