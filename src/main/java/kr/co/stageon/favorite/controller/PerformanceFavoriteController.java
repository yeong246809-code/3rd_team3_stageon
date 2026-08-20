package kr.co.stageon.favorite.controller;

import kr.co.stageon.favorite.service.PerformanceFavoriteService;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 공연 찜 기능을 처리하는 Controller
 *
 * 로그인한 회원 기준으로
 * 찜하기 / 찜 해제 기능을 처리한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
public class PerformanceFavoriteController {

    private final PerformanceFavoriteService favoriteService;
    private final MemberRepository memberRepository;

    /**
     * 공연 찜하기
     *
     * POST /api/favorites/{performanceId}
     */
    @PostMapping("/{performanceId}")
    public void addFavorite(
            @PathVariable Long performanceId,
            Authentication authentication
    ) {

        // 현재 로그인한 회원 조회
        Member member = findLoginMember(authentication);

        // 찜 등록
        favoriteService.addFavorite(
                member.getId(),
                performanceId
        );
    }

    /**
     * 공연 찜 해제
     *
     * DELETE /api/favorites/{performanceId}
     */
    @DeleteMapping("/{performanceId}")
    public void removeFavorite(
            @PathVariable Long performanceId,
            Authentication authentication
    ) {

        // 현재 로그인한 회원 조회
        Member member = findLoginMember(authentication);

        // 찜 삭제
        favoriteService.removeFavorite(
                member.getId(),
                performanceId
        );
    }

    /**
     * 현재 로그인한 회원 정보를 조회한다.
     */
    private Member findLoginMember(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException(
                    "로그인이 필요합니다."
            );
        }

        return memberRepository
                .findByEmail(
                        authentication.getName()
                                .trim()
                                .toLowerCase()
                )
                .filter(member ->
                        member.getStatus()
                                == Member.Status.ACTIVE
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "회원 정보를 찾을 수 없습니다."
                        )
                );
    }
}