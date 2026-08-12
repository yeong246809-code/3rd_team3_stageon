package kr.co.stageon.member.service;

import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.dto.MemberSignupRequest;
import kr.co.stageon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 회원가입과 회원 조회를 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 이메일 중복 확인
    public boolean isEmailDuplicated(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        return memberRepository.existsByEmail(
                normalizeEmail(email)
        );
    }

    // 활성 회원 이메일인지 확인
    public boolean isActiveMemberEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        return memberRepository.findByEmail(normalizeEmail(email))
                .filter(member ->
                        member.getStatus() == Member.Status.ACTIVE
                )
                .isPresent();
    }

    // 휴대전화 중복 확인
    public boolean isPhoneDuplicated(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }

        return memberRepository.countByNormalizedPhone(
                normalizePhone(phone)
        ) > 0;
    }

    // 이름과 휴대전화 번호로 마스킹된 이메일 조회
    public Optional<String> findEmailByNameAndPhone(
            String name,
            String phone
    ) {
        if (name == null || name.isBlank()
                || phone == null || phone.isBlank()) {
            return Optional.empty();
        }

        return memberRepository
                .findActiveMemberByNameAndNormalizedPhone(
                        name.trim(),
                        normalizePhone(phone)
                )
                .map(Member::getEmail)
                .map(this::maskEmail);
    }

    // 로그인한 회원 번호 조회
    public Long findMemberIdByEmail(String email) {
        return memberRepository.findByEmail(normalizeEmail(email))
                .map(Member::getId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "회원 정보를 찾을 수 없습니다."
                        )
                );
    }

    // 회원가입
    @Transactional
    public Long signup(MemberSignupRequest request) {
        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());

        validateSignup(request, email, phone);

        String encodedPassword =
                passwordEncoder.encode(request.password());

        Member member = Member.createUser(
                email,
                encodedPassword,
                request.name().trim(),
                phone,
                request.gender(),   //성별 추가
                request.birthDate() //생년월일 추가

        );

        return memberRepository.save(member).getId();
    }

    // 비밀번호 재설정
    @Transactional
    public void resetPassword(
            String email,
            String newPassword
    ) {
        Member member = memberRepository
                .findByEmail(normalizeEmail(email))
                .filter(found ->
                        found.getStatus() == Member.Status.ACTIVE
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "회원 정보를 찾을 수 없습니다."
                        )
                );

        if (passwordEncoder.matches(
                newPassword,
                member.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "기존 비밀번호와 다른 비밀번호를 입력해 주세요."
            );
        }

        member.changePassword(
                passwordEncoder.encode(newPassword)
        );
    }

    // 회원가입 정보 검증
    private void validateSignup(
            MemberSignupRequest request,
            String email,
            String phone
    ) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 이메일입니다."
            );
        }

        if (memberRepository.countByNormalizedPhone(phone) > 0) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 휴대전화 번호입니다."
            );
        }

        if (!request.isPasswordMatched()) {
            throw new IllegalArgumentException(
                    "비밀번호가 일치하지 않습니다."
            );
        }
    }

    // 이메일 앞부분 절반 마스킹
    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");

        if (atIndex <= 0) {
            return email;
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        int visibleLength = Math.max(
                2,
                localPart.length() / 2
        );

        visibleLength = Math.min(
                visibleLength,
                localPart.length()
        );

        String visible = localPart.substring(0, visibleLength);
        String masked = "*".repeat(
                localPart.length() - visibleLength
        );

        return visible + masked + domain;
    }

    // 이메일 저장 형식 통일
    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase();
    }

    // 휴대전화 저장 형식 통일
    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }

        return phone.replaceAll("[^0-9]", "");
    }
}