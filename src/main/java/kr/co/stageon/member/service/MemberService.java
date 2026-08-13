package kr.co.stageon.member.service;

import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.dto.MemberProfileResponse;
import kr.co.stageon.member.dto.MemberProfileUpdateRequest;
import kr.co.stageon.member.dto.MemberSignupRequest;
import kr.co.stageon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 회원 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 *
 * 주요 기능
 * - 이메일 / 휴대전화 중복 확인
 * - 아이디 찾기
 * - 회원가입
 * - 로그인 회원 조회
 * - 마이페이지 회원정보 조회 및 수정
 * - 비밀번호 재설정
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 이메일 중복 확인
    public boolean isEmailDuplicated(String email) {
        if (email == null || email.isBlank()) return false;
        return memberRepository.existsByEmail(normalizeEmail(email));
    }

    // 활성 회원 이메일 확인
    public boolean isActiveMemberEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return memberRepository.findByEmail(normalizeEmail(email))
                .filter(member -> member.getStatus() == Member.Status.ACTIVE)
                .isPresent();
    }

    // 휴대전화 중복 확인
    public boolean isPhoneDuplicated(String phone) {
        if (phone == null || phone.isBlank()) return false;
        return memberRepository.countByNormalizedPhone(normalizePhone(phone)) > 0;
    }

    // 이름과 휴대전화 번호로 마스킹된 이메일 조회
    public Optional<String> findEmailByNameAndPhone(String name, String phone) {
        if (name == null || name.isBlank() || phone == null || phone.isBlank()) {
            return Optional.empty();
        }

        return memberRepository
                .findActiveMemberByNameAndNormalizedPhone(name.trim(), normalizePhone(phone))
                .map(Member::getEmail)
                .map(this::maskEmail);
    }

    // 로그인 회원 번호 조회
    public Long findMemberIdByEmail(String email) {
        return findActiveMemberByEmail(email).getId();
    }

    // 회원가입
    @Transactional
    public Long signup(MemberSignupRequest request) {
        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());

        validateSignup(request, email, phone);

        String encodedPassword = passwordEncoder.encode(request.password());

        Member member = Member.createUser(
                email,
                encodedPassword,
                request.name().trim(),
                phone,
                request.gender(),
                request.birthDate()
        );

        return memberRepository.save(member).getId();
    }

    // 마이페이지 회원정보 조회
    public MemberProfileResponse findProfileByEmail(String email) {
        Member member = findActiveMemberByEmail(email);
        return MemberProfileResponse.from(member);
    }

    /**
     * 마이페이지 회원정보 수정
     *
     * 회원정보 수정 페이지는 본인 인증을 완료한 사용자만 접근할 수 있습니다.
     *
     * 추가 보안 정책
     * - 비밀번호 변경 시 현재 비밀번호를 다시 확인합니다.
     * - 이메일은 로그인 아이디이므로 변경하지 않습니다.
     */
    @Transactional
    public void updateProfile(
            String loginEmail,
            MemberProfileUpdateRequest request,
            boolean identityVerified
    ) {

        // =========================================================
        // 현재 로그인 회원 조회
        // =========================================================

        Member member =
                findActiveMemberByEmail(loginEmail);


        // =========================================================
        // 회원정보 수정 페이지 접근 인증 확인
        // =========================================================

        /*
         * Controller에서 세션 인증을 먼저 확인하지만,
         * 직접 POST 요청을 보내는 경우까지 막기 위해
         * Service에서도 다시 확인합니다.
         */
        if (!identityVerified) {
            throw new IllegalArgumentException(
                    "본인 인증 후 회원정보를 수정할 수 있습니다."
            );
        }


        // =========================================================
        // 이름 검증
        // =========================================================

        if (
                request.name() == null
                        || request.name().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "이름을 입력해 주세요."
            );
        }


        // =========================================================
        // 휴대전화 검증
        // =========================================================

        if (
                request.phone() == null
                        || request.phone().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "휴대전화 번호를 입력해 주세요."
            );
        }


        /*
         * 화면에서는
         *
         * 010-1234-5678
         *
         * 처럼 입력하더라도 DB에는 숫자만 저장합니다.
         */
        String normalizedPhone =
                normalizePhone(
                        request.phone()
                );


        /*
         * 국내 휴대전화 기본 형식
         *
         * 010 + 숫자 8자리
         *
         * 예)
         * 01012345678
         */
        if (
                !normalizedPhone.matches(
                        "^010\\d{8}$"
                )
        ) {

            throw new IllegalArgumentException(
                    "휴대전화 번호를 올바르게 입력해 주세요."
            );
        }


        // =========================================================
        // 휴대전화 변경 여부 확인
        // =========================================================

        boolean phoneChanged =
                !normalizedPhone.equals(
                        normalizePhone(
                                member.getPhone()
                        )
                );


        /*
         * 전화번호가 실제로 변경되는 경우에만
         * 다른 회원과 중복되는지 확인합니다.
         *
         * 기존 전화번호 그대로 저장할 경우에는
         * 자기 자신의 전화번호 때문에 중복으로 판단하면 안 됩니다.
         */
        if (
                phoneChanged
                        && memberRepository
                        .countByNormalizedPhone(
                                normalizedPhone
                        ) > 0
        ) {

            throw new IllegalArgumentException(
                    "이미 사용 중인 휴대전화 번호입니다."
            );
        }


        // =========================================================
        // 기본 회원정보 수정
        // =========================================================

        member.updateProfile(
                request.name().trim(),
                normalizedPhone,
                request.gender(),
                request.birthDate()
        );


        // =========================================================
        // 비밀번호 변경
        // =========================================================

        /*
         * 새 비밀번호가 입력된 경우에만
         * 비밀번호 변경 로직을 실행합니다.
         */
        if (request.hasNewPassword()) {


            // -----------------------------------------------------
            // 현재 비밀번호 입력 확인
            // -----------------------------------------------------

            if (
                    request.currentPassword() == null
                            || request.currentPassword().isBlank()
            ) {

                throw new IllegalArgumentException(
                        "현재 비밀번호를 입력해 주세요."
                );
            }


            // -----------------------------------------------------
            // 현재 비밀번호 일치 확인
            // -----------------------------------------------------

            if (
                    !passwordEncoder.matches(
                            request.currentPassword(),
                            member.getPasswordHash()
                    )
            ) {

                throw new IllegalArgumentException(
                        "현재 비밀번호가 일치하지 않습니다."
                );
            }


            // -----------------------------------------------------
            // 새 비밀번호 확인값 일치
            // -----------------------------------------------------

            if (!request.isPasswordMatched()) {

                throw new IllegalArgumentException(
                        "새 비밀번호가 일치하지 않습니다."
                );
            }


            // -----------------------------------------------------
            // 기존 비밀번호와 동일한지 확인
            // -----------------------------------------------------

            if (
                    passwordEncoder.matches(
                            request.newPassword(),
                            member.getPasswordHash()
                    )
            ) {

                throw new IllegalArgumentException(
                        "기존 비밀번호와 다른 비밀번호를 입력해 주세요."
                );
            }


            // -----------------------------------------------------
            // 새 비밀번호 암호화 후 변경
            // -----------------------------------------------------

            member.changePassword(
                    passwordEncoder.encode(
                            request.newPassword()
                    )
            );
        }


        /*
         * memberRepository.save(member)를 호출하지 않아도 됩니다.
         *
         * 현재 메서드가 @Transactional 상태이고
         * member는 JPA 영속성 컨텍스트가 관리하고 있으므로
         * 트랜잭션 종료 시 Dirty Checking으로 UPDATE가 실행됩니다.
         */
    }

    // 비밀번호 재설정
    @Transactional
    public void resetPassword(String email, String newPassword) {
        Member member = findActiveMemberByEmail(email);

        if (passwordEncoder.matches(newPassword, member.getPasswordHash())) {
            throw new IllegalArgumentException("기존 비밀번호와 다른 비밀번호를 입력해 주세요.");
        }

        member.changePassword(passwordEncoder.encode(newPassword));
    }

    // 공통 ACTIVE 회원 조회
    private Member findActiveMemberByEmail(String email) {
        return memberRepository.findByEmail(normalizeEmail(email))
                .filter(member -> member.getStatus() == Member.Status.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    }

    // 회원가입 정보 검증
    private void validateSignup(MemberSignupRequest request, String email, String phone) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (memberRepository.countByNormalizedPhone(phone) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 휴대전화 번호입니다.");
        }

        if (!request.isPasswordMatched()) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
    }

    // 이메일 앞부분 일부 마스킹
    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 0) return email;

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        int visibleLength = Math.max(2, localPart.length() / 2);
        visibleLength = Math.min(visibleLength, localPart.length());

        String visible = localPart.substring(0, visibleLength);
        String masked = "*".repeat(localPart.length() - visibleLength);

        return visible + masked + domain;
    }

    // 이메일 저장 형식 통일
    private String normalizeEmail(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase();
    }

    // 휴대전화 저장 형식 통일
    private String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }
}