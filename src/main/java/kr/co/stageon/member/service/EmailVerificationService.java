package kr.co.stageon.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 회원가입 이메일 인증번호 발송과 검증을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {


    private static final int EXPIRE_MINUTES = 3;
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String senderEmail;

    /*
     * 이메일별 인증 정보를 임시 저장합니다.
     * 현재는 개발용 메모리 저장 방식입니다.
     */

    private final Map<String, VerificationInfo> verificationStore =
            new ConcurrentHashMap<>();

    // 인증번호 생성 및 이메일 발송
    public void sendVerificationCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        String verificationCode = createVerificationCode();

        VerificationInfo verificationInfo = new VerificationInfo(
                verificationCode,
                LocalDateTime.now().plusMinutes(EXPIRE_MINUTES),
                false
        );

        verificationStore.put(normalizedEmail, verificationInfo);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setFrom(senderEmail, "StageOn");
            helper.setTo(normalizedEmail);
            helper.setSubject("[StageOn] 회원가입 이메일 인증번호");
            helper.setText("""
                StageOn 회원가입 인증번호입니다.

                인증번호: %s

                인증번호는 3분 동안 유효합니다.
                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(verificationCode));

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            verificationStore.remove(normalizedEmail);

            throw new IllegalStateException(
                    "인증 메일 발송에 실패했습니다.",
                    e
            );
        }
    }

    // 인증번호 검증
    public boolean verifyCode(String email, String inputCode) {
        String normalizedEmail = normalizeEmail(email);
        VerificationInfo verificationInfo =
                verificationStore.get(normalizedEmail);

        if (verificationInfo == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(verificationInfo.expiresAt())) {
            verificationStore.remove(normalizedEmail);
            return false;
        }

        if (!verificationInfo.code().equals(inputCode)) {
            return false;
        }

        verificationStore.put(
                normalizedEmail,
                new VerificationInfo(
                        verificationInfo.code(),
                        verificationInfo.expiresAt(),
                        true
                )
        );

        return true;
    }

    // 인증 완료 여부
    public boolean isVerified(String email) {
        String normalizedEmail = normalizeEmail(email);
        VerificationInfo verificationInfo =
                verificationStore.get(normalizedEmail);

        if (verificationInfo == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(verificationInfo.expiresAt())) {
            verificationStore.remove(normalizedEmail);
            return false;
        }

        return verificationInfo.verified();
    }

    // 인증 정보 삭제
    public void clearVerification(String email) {
        verificationStore.remove(normalizeEmail(email));
    }

    // 6자리 인증번호 생성
    private String createVerificationCode() {
        SecureRandom random = new SecureRandom();
        int min = 100000;
        int max = 999999;

        return String.valueOf(
                random.nextInt(max - min + 1) + min
        );
    }

    // 이메일 형식 통일
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private record VerificationInfo(
            String code,
            LocalDateTime expiresAt,
            boolean verified
    ) {
    }
}