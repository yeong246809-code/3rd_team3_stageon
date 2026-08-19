package kr.co.stageon.admin.dto;

/** 수동 환불 모달의 회원 검색 자동완성 결과 한 건입니다. */
public record AdminMemberSearchItemDto(
        Long memberId,
        String name,
        String email,
        String maskedPhone
) {
}