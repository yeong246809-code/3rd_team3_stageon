package kr.co.stageon.member.controller;

import kr.co.stageon.member.service.EmailVerificationService;
import kr.co.stageon.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 회원가입 이메일 인증 요청을 처리합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/email-verification")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final MemberService memberService;

    // 인증번호 발송
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendCode(
            @RequestBody EmailSendRequest request
    ) {
        String email = request.email();

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "이메일을 입력해 주세요."
                    )
            );
        }

        if (memberService.isEmailDuplicated(email)) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "이미 사용 중인 이메일입니다."
                    )
            );
        }

        try {
            emailVerificationService.sendVerificationCode(email);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "인증번호를 이메일로 발송했습니다."
                    )
            );

        } catch (Exception e) {
            log.error("이메일 인증번호 발송 실패: {}", email, e);

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message", "인증번호 발송에 실패했습니다."
                    )
            );
        }
    }

    // 인증번호 확인
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyCode(
            @RequestBody EmailVerifyRequest request
    ) {
        if (request.email() == null || request.email().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "이메일을 입력해 주세요."
                    )
            );
        }

        if (request.code() == null || request.code().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "인증번호를 입력해 주세요."
                    )
            );
        }

        boolean verified =
                emailVerificationService.verifyCode(
                        request.email(),
                        request.code()
                );

        if (!verified) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "인증번호가 일치하지 않거나 만료되었습니다."
                    )
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "이메일 인증이 완료되었습니다."
                )
        );
    }

    // 인증번호 발송 요청
    public record EmailSendRequest(
            String email
    ) {
    }

    // 인증번호 검증 요청
    public record EmailVerifyRequest(
            String email,
            String code
    ) {
    }
}