package kr.co.stageon.queue.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class QueueTokenService {

    public String issue() {
        return UUID.randomUUID().toString();
    }

    public String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(rawToken.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("대기열 토큰 해시 생성에 실패했습니다.", exception);
        }
    }
}
