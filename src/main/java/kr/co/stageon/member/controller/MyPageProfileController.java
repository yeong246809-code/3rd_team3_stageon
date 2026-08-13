package kr.co.stageon.member.controller;

import jakarta.servlet.http.HttpSession;
import kr.co.stageon.member.dto.MemberProfileUpdateRequest;
import kr.co.stageon.member.service.EmailVerificationService;
import kr.co.stageon.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 마이페이지 회원정보 수정 기능을 담당합니다.
 *
 * 흐름
 *
 * 1. 사용자가 /mypage/profile 접속
 * 2. 본인 인증 여부 확인
 * 3. 인증 전이면 본인 인증 화면 표시
 * 4. 이메일 인증번호 확인 성공
 * 5. 세션에 본인 인증 완료 상태 저장
 * 6. /mypage/profile 재접속
 * 7. 실제 회원정보 수정 화면 표시
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage/profile")
public class MyPageProfileController {

    private final MemberService memberService;
    private final EmailVerificationService emailVerificationService;


    // =========================================================
    // 본인 인증 세션 키
    // =========================================================

    /**
     * 본인 인증을 완료한 이메일을 저장합니다.
     */
    private static final String PROFILE_VERIFIED_EMAIL =
            "PROFILE_VERIFIED_EMAIL";


    /**
     * 본인 인증 완료 시각을 저장합니다.
     */
    private static final String PROFILE_VERIFIED_AT =
            "PROFILE_VERIFIED_AT";


    /**
     * 회원정보 수정 페이지 접근 인증 유효시간
     *
     * 현재는 인증 완료 후 10분 동안
     * 회원정보 수정 화면에 접근할 수 있도록 합니다.
     */
    private static final long PROFILE_VERIFICATION_VALID_MILLIS =
            10 * 60 * 1000L;


    // =========================================================
    // 회원정보 수정 화면 진입
    // =========================================================

    /**
     * 회원정보 수정 화면으로 진입합니다.
     *
     * 인증을 완료하지 않은 회원은 실제 회원정보를 보여주지 않고
     * 먼저 본인 인증 화면으로 이동합니다.
     */
    @GetMapping
    public String profile(
            Authentication authentication,
            HttpSession session,
            Model model
    ) {

        /*
         * 아직 본인 인증을 완료하지 않은 경우
         *
         * 실제 회원정보 수정 페이지에 진입시키지 않고
         * 본인 인증 화면을 먼저 보여줍니다.
         */
        if (!isProfileIdentityVerified(
                authentication,
                session
        )) {

            /*
             * 인증 화면에는 현재 로그인 이메일만 전달합니다.
             *
             * Member 전체 정보는 인증 전에는 전달하지 않습니다.
             */
            model.addAttribute(
                    "email",
                    authentication.getName()
            );

            return "user/mypage-profile-auth";
        }


        /*
         * 본인 인증 완료 후에만
         * 실제 회원정보를 조회합니다.
         */
        model.addAttribute(
                "profile",
                memberService.findProfileByEmail(
                        authentication.getName()
                )
        );


        return "user/mypage-profile";
    }


    // =========================================================
    // 회원정보 수정
    // =========================================================

    /**
     * 회원정보 수정 요청을 처리합니다.
     *
     * 이 메서드에서도 다시 인증 여부를 확인합니다.
     *
     * 화면만 막는 것이 아니라
     * 직접 POST 요청을 보내더라도 수정되지 않도록
     * 서버에서 다시 검증합니다.
     */
    @PostMapping
    public String updateProfile(
            Authentication authentication,
            MemberProfileUpdateRequest request,
            HttpSession session,
            Model model
    ) {

        /*
         * 인증 세션이 없거나 만료된 경우
         * 회원정보 수정 자체를 진행하지 않습니다.
         */
        if (!isProfileIdentityVerified(
                authentication,
                session
        )) {

            model.addAttribute(
                    "email",
                    authentication.getName()
            );

            model.addAttribute(
                    "error",
                    "본인 인증 시간이 만료되었습니다. 다시 인증해 주세요."
            );

            return "user/mypage-profile-auth";
        }


        try {

            /*
             * 회원정보 수정
             *
             * 페이지 진입 자체가 이미 본인 인증 완료 상태이므로
             * Service에는 true를 전달합니다.
             */
            memberService.updateProfile(
                    authentication.getName(),
                    request,
                    true
            );


            /*
             * 수정 완료 후
             * 최신 회원정보를 다시 조회합니다.
             */
            model.addAttribute(
                    "profile",
                    memberService.findProfileByEmail(
                            authentication.getName()
                    )
            );


            model.addAttribute(
                    "success",
                    "회원정보가 수정되었습니다."
            );


        } catch (IllegalArgumentException e) {

            /*
             * 입력값 검증 실패 시
             * 동일한 회원정보 수정 화면을 다시 표시합니다.
             */
            model.addAttribute(
                    "profile",
                    memberService.findProfileByEmail(
                            authentication.getName()
                    )
            );


            model.addAttribute(
                    "error",
                    e.getMessage()
            );
        }


        /*
         * 여기서는 인증 세션을 바로 삭제하지 않습니다.
         *
         * 사용자가 입력 오류를 수정하거나
         * 추가 정보를 변경할 수 있도록
         * 10분 동안 인증 상태를 유지합니다.
         */
        return "user/mypage-profile";
    }


    // =========================================================
    // 인증번호 발송
    // =========================================================

    /**
     * 현재 로그인 회원의 이메일로
     * 본인 인증번호를 발송합니다.
     */
    @ResponseBody
    @PostMapping("/verification/send")
    public ResponseEntity<Map<String, Object>>
    sendVerificationCode(
            Authentication authentication,
            HttpSession session
    ) {

        String loginEmail =
                authentication.getName();


        /*
         * 새로운 인증번호를 발송하면
         * 기존 본인 인증 세션은 초기화합니다.
         */
        clearProfileVerification(
                session
        );


        /*
         * 기존 EmailVerificationService 인증정보도
         * 한 번 초기화합니다.
         */
        emailVerificationService
                .clearVerification(
                        loginEmail
                );


        try {

            /*
             * 기존 이메일 인증 서비스 재사용
             */
            emailVerificationService
                    .sendVerificationCode(
                            loginEmail
                    );


            return ResponseEntity.ok(
                    Map.of(
                            "success",
                            true,

                            "message",
                            "현재 계정 이메일로 인증번호를 발송했습니다."
                    )
            );


        } catch (Exception e) {

            log.error(
                    "마이페이지 본인 인증번호 발송 실패: {}",
                    loginEmail,
                    e
            );


            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of(
                                    "success",
                                    false,

                                    "message",
                                    "인증번호 발송에 실패했습니다."
                            )
                    );
        }
    }


    // =========================================================
    // 인증번호 확인
    // =========================================================

    /**
     * 사용자가 입력한 6자리 인증번호를 확인합니다.
     */
    @ResponseBody
    @PostMapping("/verification/verify")
    public ResponseEntity<Map<String, Object>>
    verifyVerificationCode(
            @RequestBody VerificationCodeRequest request,
            Authentication authentication,
            HttpSession session
    ) {

        /*
         * 인증번호 입력 확인
         */
        if (request.code() == null
                || request.code().isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "success",
                                    false,

                                    "message",
                                    "인증번호를 입력해 주세요."
                            )
                    );
        }


        String loginEmail =
                authentication.getName();


        /*
         * 기존 EmailVerificationService를 이용해
         * 인증번호를 검증합니다.
         */
        boolean verified =
                emailVerificationService
                        .verifyCode(
                                loginEmail,
                                request.code()
                        );


        /*
         * 인증 실패
         */
        if (!verified) {

            clearProfileVerification(
                    session
            );


            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "success",
                                    false,

                                    "message",
                                    "인증번호가 일치하지 않거나 만료되었습니다."
                            )
                    );
        }


        // =====================================================
        // 인증 성공
        // =====================================================

        /*
         * 현재 로그인 이메일 저장
         */
        session.setAttribute(
                PROFILE_VERIFIED_EMAIL,
                loginEmail
                        .trim()
                        .toLowerCase()
        );


        /*
         * 인증 성공 시각 저장
         */
        session.setAttribute(
                PROFILE_VERIFIED_AT,
                System.currentTimeMillis()
        );


        return ResponseEntity.ok(
                Map.of(
                        "success",
                        true,

                        "message",
                        "본인 인증이 완료되었습니다."
                )
        );
    }


    // =========================================================
    // 인증 상태 확인
    // =========================================================

    /**
     * 현재 세션의 본인 인증 상태가 유효한지 확인합니다.
     *
     * 확인 조건
     *
     * 1. 인증 이메일이 존재하는가
     * 2. 인증 완료 시간이 존재하는가
     * 3. 현재 로그인 이메일과 같은가
     * 4. 인증 완료 후 10분 이내인가
     */
    private boolean isProfileIdentityVerified(
            Authentication authentication,
            HttpSession session
    ) {

        Object verifiedEmailObject =
                session.getAttribute(
                        PROFILE_VERIFIED_EMAIL
                );


        Object verifiedAtObject =
                session.getAttribute(
                        PROFILE_VERIFIED_AT
                );


        /*
         * 인증 정보가 없는 경우
         */
        if (!(verifiedEmailObject
                instanceof String verifiedEmail)
                || !(verifiedAtObject
                instanceof Long verifiedAt)) {

            return false;
        }


        String loginEmail =
                authentication
                        .getName()
                        .trim()
                        .toLowerCase();


        /*
         * 다른 계정에서 생성된 인증 정보라면
         * 사용할 수 없습니다.
         */
        if (!loginEmail.equals(
                verifiedEmail
        )) {

            clearProfileVerification(
                    session
            );

            return false;
        }


        long elapsed =
                System.currentTimeMillis()
                        - verifiedAt;


        /*
         * 인증 유효시간 10분 초과
         */
        if (elapsed
                > PROFILE_VERIFICATION_VALID_MILLIS) {

            clearProfileVerification(
                    session
            );

            emailVerificationService
                    .clearVerification(
                            loginEmail
                    );

            return false;
        }


        return true;
    }


    // =========================================================
    // 인증 세션 초기화
    // =========================================================

    /**
     * 회원정보 수정용 본인 인증 상태를 삭제합니다.
     */
    private void clearProfileVerification(
            HttpSession session
    ) {

        session.removeAttribute(
                PROFILE_VERIFIED_EMAIL
        );


        session.removeAttribute(
                PROFILE_VERIFIED_AT
        );
    }


    // =========================================================
    // 인증번호 요청 DTO
    // =========================================================

    /**
     * 인증번호 확인 API 요청값입니다.
     */
    public record VerificationCodeRequest(
            String code
    ) {
    }
}