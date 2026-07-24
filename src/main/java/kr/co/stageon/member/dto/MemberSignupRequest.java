package kr.co.stageon.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 회원가입 입력값입니다. 비밀번호는 Service에서 해시한 후에만 저장합니다. */
public record MemberSignupRequest(
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(max = 80) String name,
        @Pattern(regexp = "^[0-9-]{0,30}$") String phone,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
