package kr.co.stageon.member.service;

import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.dto.MemberSignupRequest;
import kr.co.stageon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입과 회원 중복검사를 처리합니다.
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

    // 휴대전화 중복 확인
    public boolean isPhoneDuplicated(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }

        return memberRepository.countByNormalizedPhone(
                normalizePhone(phone)
        ) > 0;
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
                phone
        );

        return memberRepository.save(member).getId();
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

    // 이메일 저장 형식 통일
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    // 휴대전화 저장 형식 통일
    private String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }
}