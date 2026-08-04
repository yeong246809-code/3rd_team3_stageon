package kr.co.stageon.member.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 비밀번호 재설정 이메일 인증을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetVerificationService {

    private static final int CODE_EXPIRE_MINUTES = 3;
    private static final int VERIFIED_EXPIRE_MINUTES = 15;

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    private final Map<String, VerificationInfo> verificationStore =
            new ConcurrentHashMap<>();

    // 인증번호 발송
    public void sendVerificationCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        String code = createVerificationCode();

        verificationStore.put(
                normalizedEmail,
                new VerificationInfo(
                        code,
                        LocalDateTime.now()
                                .plusMinutes(CODE_EXPIRE_MINUTES),
                        false
                )
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            false,
                            "UTF-8"
                    );

            helper.setFrom(senderEmail, "StageOn");
            helper.setTo(normalizedEmail);
            helper.setSubject(
                    "[StageOn] 비밀번호 재설정 인증번호"
            );
            helper.setText("""
                    StageOn 비밀번호 재설정 인증번호입니다.

                    인증번호: %s

                    인증번호는 3분 동안 유효합니다.
                    본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                    """.formatted(code));

            mailSender.send(message);

        } catch (Exception e) {
            verificationStore.remove(normalizedEmail);

            throw new IllegalStateException(
                    "인증 메일 발송에 실패했습니다.",
                    e
            );
        }
    }

    // 인증번호 확인
    public boolean verifyCode(
            String email,
            String inputCode
    ) {
        String normalizedEmail = normalizeEmail(email);
        VerificationInfo info =
                verificationStore.get(normalizedEmail);

        if (info == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(info.expiresAt())) {
            verificationStore.remove(normalizedEmail);
            return false;
        }

        if (!info.code().equals(inputCode)) {
            return false;
        }

        verificationStore.put(
                normalizedEmail,
                new VerificationInfo(
                        info.code(),
                        LocalDateTime.now()
                                .plusMinutes(VERIFIED_EXPIRE_MINUTES),
                        true
                )
        );

        return true;
    }

    // 인증 완료 여부
    public boolean isVerified(String email) {
        String normalizedEmail = normalizeEmail(email);
        VerificationInfo info =
                verificationStore.get(normalizedEmail);

        if (info == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(info.expiresAt())) {
            verificationStore.remove(normalizedEmail);
            return false;
        }

        return info.verified();
    }

    // 인증 정보 삭제
    public void clearVerification(String email) {
        verificationStore.remove(normalizeEmail(email));
    }

    private String createVerificationCode() {
        SecureRandom random = new SecureRandom();

        return String.valueOf(
                random.nextInt(900000) + 100000
        );
    }

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