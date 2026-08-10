package kr.co.stageon.member.controller;

import kr.co.stageon.booking.dto.ReservationDetailResponse;
import kr.co.stageon.booking.dto.ReservationResponse;
import kr.co.stageon.booking.service.BookingQueryService;
import kr.co.stageon.booking.service.ReservationDetailQueryService;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 로그인 회원의 마이페이지 화면을 담당합니다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MypageController {

    private final MemberRepository memberRepository;
    private final BookingQueryService bookingQueryService;
    private final ReservationDetailQueryService reservationDetailQueryService;

    @GetMapping
    public String home(
            Authentication authentication,
            Model model
    ) {
        Member member = findLoginMember(authentication);

        List<ReservationResponse> reservations =
                bookingQueryService.findMemberReservations(
                        member.getId()
                );

        long upcomingCount = reservations.stream()
                .filter(this::isUpcomingReservation)
                .count();

        // 가장 가까운 다가오는 공연 1건 조회
        ReservationResponse upcomingReservation =
                bookingQueryService
                        .findNearestUpcomingReservation(member.getId())
                        .orElse(null);

        model.addAttribute("memberName", member.getName());
        model.addAttribute("memberEmail", member.getEmail());
        model.addAttribute("reservations", reservations);
        model.addAttribute("reservationCount", reservations.size());
        model.addAttribute("upcomingCount", upcomingCount);
        model.addAttribute("upcomingReservation", upcomingReservation);

        return "user/mypage";
    }

    @GetMapping("/reservations")
    public String reservations(
            Authentication authentication,
            Model model
    ) {
        Member member = findLoginMember(authentication);

        List<ReservationResponse> reservations =
                bookingQueryService.findMemberReservations(
                        member.getId()
                );

        model.addAttribute("memberName", member.getName());
        model.addAttribute("memberEmail", member.getEmail());
        model.addAttribute("reservations", reservations);
        model.addAttribute("activeMenu", "reservations");

        return "user/mypage-reservations";
    }

    @GetMapping("/reservations/{reservationId}")
    public String reservationDetail(
            @PathVariable Long reservationId,
            Authentication authentication,
            Model model
    ) {
        Member member = findLoginMember(authentication);

        // 예매·좌석·최신 결제 정보를 상세 화면 전용 DTO로 조회합니다.
        ReservationDetailResponse reservation =
                reservationDetailQueryService.findDetail(
                        reservationId,
                        member.getId()
                );

        model.addAttribute("reservation", reservation);
        model.addAttribute("activeMenu", "reservations");

        return "user/mypage-reservation-detail";
    }

    @GetMapping("/tickets")
    public String tickets(Model model) {
        return placeholder(
                model,
                "tickets",
                "보유 티켓",
                "사용 가능한 모바일 티켓을 확인합니다."
        );
    }

    @GetMapping("/reviews")
    public String reviews(Model model) {
        return placeholder(
                model,
                "reviews",
                "관람 후기",
                "관람한 공연의 후기와 평점을 관리합니다."
        );
    }

    @GetMapping({"/points", "/coupons"})
    public String benefits(Model model) {
        return placeholder(
                model,
                "points",
                "포인트·쿠폰",
                "보유 포인트와 사용 가능한 쿠폰을 확인합니다."
        );
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        return placeholder(
                model,
                "profile",
                "회원정보 수정",
                "이름과 휴대전화 번호 등 회원 정보를 관리합니다."
        );
    }

    private Member findLoginMember(
            Authentication authentication
    ) {
        return memberRepository
                .findByEmail(
                        authentication.getName()
                                .trim()
                                .toLowerCase()
                )
                .filter(member ->
                        member.getStatus() == Member.Status.ACTIVE
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "회원 정보를 찾을 수 없습니다."
                        )
                );
    }

    private boolean isUpcomingReservation(
            ReservationResponse reservation
    ) {
        if (reservation.startsAt() == null) {
            return false;
        }

        if (reservation.status().equals("CANCELLED")) {
            return false;
        }

        return reservation.startsAt()
                .isAfter(LocalDateTime.now());
    }

    private String placeholder(
            Model model,
            String activeMenu,
            String pageTitle,
            String pageDescription
    ) {
        model.addAttribute("activeMenu", activeMenu);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageDescription", pageDescription);


        return "user/mypage-placeholder";
    }
}