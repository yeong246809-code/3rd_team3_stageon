package kr.co.stageon.member.controller;

import kr.co.stageon.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * 회원 정보 확인 요청을 처리합니다.
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

    // 이름과 휴대전화 번호로 가입 이메일 찾기
    @PostMapping("/api/members/find-id")
    public ResponseEntity<Map<String, Object>> findId(
            @RequestBody FindIdRequest request
    ) {
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "이름을 입력해 주세요."
                    )
            );
        }

        if (request.phone() == null || request.phone().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "휴대전화 번호를 입력해 주세요."
                    )
            );
        }

        Optional<String> email =
                memberService.findEmailByNameAndPhone(
                        request.name(),
                        request.phone()
                );

        if (email.isEmpty()) {
            return ResponseEntity.status(404).body(
                    Map.of(
                            "success", false,
                            "message", "입력한 정보와 일치하는 회원을 찾을 수 없습니다."
                    )
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "email", email.get(),
                        "message", "가입 이메일을 확인했습니다."
                )
        );
    }

    public record FindIdRequest(
            String name,
            String phone
    ) {
    }
}