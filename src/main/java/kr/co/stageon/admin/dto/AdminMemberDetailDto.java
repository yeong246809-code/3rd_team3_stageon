package kr.co.stageon.admin.dto;

import kr.co.stageon.member.domain.Member;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** AD10 "회원 관리" 상세 모달용 DTO입니다. */
public record AdminMemberDetailDto(
        Long id,
        String email,
        String name,
        String phone,
        String gender,
        LocalDate birthDate,
        Member.Role role,
        Member.Status status,
        String adminMemo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}