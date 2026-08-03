package kr.co.stageon.member.controller;

import kr.co.stageon.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 회원가입 중복 확인 요청을 처리합니다.
 */
@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    // 이메일 중복 확인
    @GetMapping("/api/members/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(
            @RequestParam String email
    ) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "duplicated", false,
                            "message", "이메일을 입력해 주세요."
                    )
            );
        }

        boolean duplicated =
                memberService.isEmailDuplicated(email);

        return ResponseEntity.ok(
                Map.of(
                        "duplicated", duplicated,
                        "message", duplicated
                                ? "이미 사용 중인 이메일입니다."
                                : "사용 가능한 이메일입니다."
                )
        );
    }

    // 휴대전화 중복 확인
    @GetMapping("/api/members/check-phone")
    public ResponseEntity<Map<String, Object>> checkPhone(
            @RequestParam String phone
    ) {
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "duplicated", false,
                            "message", "휴대전화 번호를 입력해 주세요."
                    )
            );
        }

        boolean duplicated =
                memberService.isPhoneDuplicated(phone);

        return ResponseEntity.ok(
                Map.of(
                        "duplicated", duplicated,
                        "message", duplicated
                                ? "이미 사용 중인 휴대전화 번호입니다."
                                : "사용 가능한 휴대전화 번호입니다."
                )
        );
    }
}