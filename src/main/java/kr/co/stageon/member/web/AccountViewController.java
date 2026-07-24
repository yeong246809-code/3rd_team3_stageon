package kr.co.stageon.member.web;

import kr.co.stageon.booking.service.BookingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** 로그인·회원가입·마이페이지 화면 진입을 담당합니다. */
@Controller
@RequiredArgsConstructor
public class AccountViewController {

    private final BookingQueryService bookingQueryService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "auth/signup";
    }

    @GetMapping("/mypage")
    public String mypage(@RequestParam(required = false) Long memberId, Model model) {
        model.addAttribute("reservations",
                memberId == null ? List.of() : bookingQueryService.findMemberReservations(memberId));
        return "user/mypage";
    }
}
