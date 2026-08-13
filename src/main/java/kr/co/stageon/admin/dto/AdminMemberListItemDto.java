package kr.co.stageon.admin.dto;

import kr.co.stageon.member.domain.Member;

import java.time.LocalDateTime;

/** AD10 "회원 관리" 목록 화면의 행 하나를 나타냅니다. */
public record AdminMemberListItemDto(
        Long id,
        String email,
        String name,
        String maskedPhone,
        Member.Role role,
        Member.Status status,
        LocalDateTime createdAt
) {
}