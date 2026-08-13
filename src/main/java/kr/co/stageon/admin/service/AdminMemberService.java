package kr.co.stageon.admin.service;

import kr.co.stageon.admin.dto.AdminMemberDetailDto;
import kr.co.stageon.admin.dto.AdminMemberListItemDto;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AD10 "회원 관리" 목록·상세 조회, 권한 변경, 메모 수정을 담당합니다. */
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final MemberRepository memberRepository;

    /** 이름/이메일 검색, 권한, 상태로 필터링한 회원 목록을 페이지 단위로 조회합니다. */
    @Transactional(readOnly = true)
    public Page<AdminMemberListItemDto> search(String keyword, Member.Role role, Member.Status status, Pageable pageable) {
        Page<Member> page = memberRepository.search(
                role,
                status,
                (keyword == null || keyword.isBlank()) ? null : keyword,
                pageable
        );

        return page.map(m -> new AdminMemberListItemDto(
                m.getId(),
                m.getEmail(),
                m.getName(),
                maskPhone(m.getPhone()),
                m.getRole(),
                m.getStatus(),
                m.getCreatedAt()
        ));
    }

    /** 회원 상세 정보(마스킹 없이 전체 항목)를 조회합니다. */
    @Transactional(readOnly = true)
    public AdminMemberDetailDto getDetail(Long id) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return new AdminMemberDetailDto(
                m.getId(), m.getEmail(), m.getName(), m.getPhone(), m.getGender(), m.getBirthDate(),
                m.getRole(), m.getStatus(), m.getAdminMemo(), m.getCreatedAt(), m.getUpdatedAt()
        );
    }

    /** 회원 권한을 변경합니다. 관리자 본인 계정 변경 방지는 컨트롤러에서 처리합니다. */
    @Transactional
    public void changeRole(Long id, Member.Role newRole) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        m.changeRole(newRole);
    }

    /** 관리자 메모를 수정합니다. */
    @Transactional
    public void updateMemo(Long id, String memo) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        m.updateAdminMemo(memo);
    }

    /** 전화번호 마스킹: 뒷자리 4자리를 '*'로 표시합니다. */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return phone.substring(0, phone.length() - 4) + "****";
    }
}