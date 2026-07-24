package kr.co.stageon.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 세션 로그인 요청 DTO입니다. */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
