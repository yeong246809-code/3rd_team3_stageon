package kr.co.stageon.member.controller;

import kr.co.stageon.member.service.MemberService;
import kr.co.stageon.member.service.PasswordResetVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 비밀번호 재설정 요청을 처리합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/password-reset")
public class PasswordResetController {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile(
                    "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,20}$"
            );

    private final MemberService memberService;
    private final PasswordResetVerificationService verificationService;

    // 가입 이메일 확인
    @PostMapping("/check-account")
    public ResponseEntity<Map<String, Object>> checkAccount(
            @RequestBody EmailRequest request
    ) {
        if (request.email() == null
                || request.email().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "이메일을 입력해 주세요."
                    )
            );
        }

        if (!memberService.isActiveMemberEmail(request.email())) {
            return ResponseEntity.status(404).body(
                    Map.of(
                            "success", false,
                            "message", "가입된 활성 계정을 찾을 수 없습니다."
                    )
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "가입된 계정을 확인했습니다."
                )
        );
    }

    // 인증번호 발송
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendCode(
            @RequestBody EmailRequest request
    ) {
        String email = request.email();

        if (!memberService.isActiveMemberEmail(email)) {
            return ResponseEntity.status(404).body(
                    Map.of(
                            "success", false,
                            "message", "가입된 활성 계정을 찾을 수 없습니다."
                    )
            );
        }

        try {
            verificationService.sendVerificationCode(email);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "인증번호를 이메일로 발송했습니다."
                    )
            );

        } catch (Exception e) {
            log.error(
                    "비밀번호 재설정 인증번호 발송 실패: {}",
                    email,
                    e
            );

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
            @RequestBody VerifyRequest request
    ) {
        if (request.email() == null
                || request.email().isBlank()
                || request.code() == null
                || request.code().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "이메일과 인증번호를 입력해 주세요."
                    )
            );
        }

        boolean verified = verificationService.verifyCode(
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

    // 새 비밀번호 저장
    @PostMapping("/complete")
    public ResponseEntity<Map<String, Object>> complete(
            @RequestBody ResetPasswordRequest request
    ) {
        if (!verificationService.isVerified(request.email())) {
            return ResponseEntity.status(403).body(
                    Map.of(
                            "success", false,
                            "message", "이메일 인증을 먼저 완료해 주세요."
                    )
            );
        }

        if (request.newPassword() == null
                || !PASSWORD_PATTERN
                .matcher(request.newPassword())
                .matches()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message",
                            "비밀번호는 영문·숫자·특수문자를 포함한 8~20자로 입력해 주세요."
                    )
            );
        }

        if (!request.newPassword().equals(
                request.newPasswordConfirm()
        )) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "비밀번호가 일치하지 않습니다."
                    )
            );
        }

        try {
            memberService.resetPassword(
                    request.email(),
                    request.newPassword()
            );

            verificationService.clearVerification(
                    request.email()
            );

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "비밀번호가 변경되었습니다."
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }

    public record EmailRequest(String email) {
    }

    public record VerifyRequest(
            String email,
            String code
    ) {
    }

    public record ResetPasswordRequest(
            String email,
            String newPassword,
            String newPasswordConfirm
    ) {
    }
}