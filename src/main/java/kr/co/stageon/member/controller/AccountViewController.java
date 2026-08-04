package kr.co.stageon.member.controller;

import kr.co.stageon.booking.service.BookingQueryService;
import kr.co.stageon.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 로그인·회원가입·계정 찾기·마이페이지 화면 진입을 담당합니다.
 */
@Controller
@RequiredArgsConstructor
public class AccountViewController {

    private final BookingQueryService bookingQueryService;
    private final MemberService memberService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "auth/signup";
    }

    @GetMapping("/find-id")
    public String findId() {
        return "auth/find-id";
    }

    @GetMapping("/password-reset")
    public String passwordReset() {
        return "auth/password-reset";
    }


}