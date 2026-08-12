package kr.co.stageon.member.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

/**
 * 회원가입 요청 정보를 전달합니다.
 */
public record MemberSignupRequest(

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "example@email.com 형식으로 입력해 주세요."
        )
        @Size(max = 190)
        String email,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 80)
        String name,

        @NotBlank(message = "휴대전화 번호를 입력해 주세요.")
        @Pattern(
                regexp = "^010-?\\d{4}-?\\d{4}$",
                message = "휴대전화 번호 형식이 올바르지 않습니다."
        )
        String phone,

        // 성별
        @NotBlank(message = "성별을 선택해 주세요.")
        String gender,

        // 생년월일
        @NotNull(message = "생년월일을 입력해 주세요.")
        LocalDate birthDate,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(
                min = 8,
                max = 20,
                message = "비밀번호는 8~20자로 입력해 주세요."
        )
        String password,

        @NotBlank(message = "비밀번호 확인을 입력해 주세요.")
        String passwordConfirm,

        @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
        boolean serviceTerms,

        @AssertTrue(message = "개인정보 수집·이용에 동의해야 합니다.")
        boolean privacyTerms,

        @AssertTrue(message = "만 14세 이상만 가입할 수 있습니다.")
        boolean ageConfirmed,

        Boolean marketingTerms

) {

    // 비밀번호 일치 여부
    public boolean isPasswordMatched() {
        return password != null && password.equals(passwordConfirm);
    }
}