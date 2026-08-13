package kr.co.stageon.member.dto;

import kr.co.stageon.member.domain.Member;

import java.time.LocalDate;

/**
 * 마이페이지 회원정보 수정 화면에
 * 현재 회원 정보를 전달하는 DTO입니다.
 */
public record MemberProfileResponse(

        // 로그인 아이디로 사용하는 이메일
        String email,

        // 회원 이름
        String name,

        // 휴대전화 번호
        String phone,

        // 성별
        String gender,

        // 생년월일
        LocalDate birthDate

) {

    /**
     * Member Entity를
     * 회원정보 수정 화면용 DTO로 변환합니다.
     */
    public static MemberProfileResponse from(Member member) {

        return new MemberProfileResponse(
                member.getEmail(),
                member.getName(),
                member.getPhone(),
                member.getGender(),
                member.getBirthDate()
        );
    }
}