package kr.co.stageon.member.dto;

import java.time.LocalDate;

/**
 * 마이페이지 회원정보 수정 요청 DTO입니다.
 *
 * 이메일은 로그인 아이디로 사용하므로
 * 회원정보 수정 대상에서는 제외합니다.
 */
public record MemberProfileUpdateRequest(

        // 수정할 회원 이름
        String name,

        // 수정할 휴대전화 번호
        String phone,

        // 수정할 성별
        String gender,

        // 수정할 생년월일
        LocalDate birthDate,

        // 비밀번호 변경 시 확인할 현재 비밀번호
        // 비밀번호를 변경하지 않는 경우 비워둘 수 있습니다.
        String currentPassword,

        // 새 비밀번호
        String newPassword,

        // 새 비밀번호 확인
        String newPasswordConfirm

) {

    /**
     * 새 비밀번호를 입력했는지 확인합니다.
     */
    public boolean hasNewPassword() {

        return newPassword != null
                && !newPassword.isBlank();
    }


    /**
     * 새 비밀번호와
     * 새 비밀번호 확인 값이 같은지 확인합니다.
     */
    public boolean isPasswordMatched() {

        // 비밀번호를 변경하지 않는 경우
        if (!hasNewPassword()) {
            return true;
        }

        return newPassword.equals(
                newPasswordConfirm
        );
    }
}