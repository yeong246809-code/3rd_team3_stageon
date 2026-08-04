package kr.co.stageon.member.controller;

import jakarta.validation.Valid;
import kr.co.stageon.member.dto.MemberSignupRequest;
import kr.co.stageon.member.service.EmailVerificationService;
import kr.co.stageon.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class SignupController {

    private final MemberService memberService;
    private final EmailVerificationService emailVerificationService;

    // 회원가입 처리
    @PostMapping("/signup")
    public String signup(
            @Valid MemberSignupRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        if (!emailVerificationService.isVerified(request.email())) {
            bindingResult.reject(
                    "emailVerification",
                    "이메일 인증을 완료해 주세요."
            );

            return "auth/signup";
        }

        try {
            memberService.signup(request);
            emailVerificationService.clearVerification(request.email());

            return "redirect:/signup/complete";

        } catch (IllegalArgumentException e) {
            bindingResult.reject(
                    "signupFailed",
                    e.getMessage()
            );

            return "auth/signup";
        }
    }

    // 회원가입 완료 화면
    @GetMapping("/signup/complete")
    public String signupComplete() {
        return "auth/signup-complete";
    }
}