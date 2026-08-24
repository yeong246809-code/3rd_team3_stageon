package kr.co.stageon.ticket.web;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TicketApiController {

    private final RedissonClient redissonClient;

    // 1. 관리자 스캐너 화면 띄우기 (Step 3에서 사용할 화면)
    @GetMapping("/admin/scanner")
    public String scannerPage() {
        return "admin/scanner"; // admin 폴더 안의 scanner.html 렌더링
    }

    // 2. 관객에게 30초짜리 동적 QR 토큰 발급
    @ResponseBody
    @GetMapping("/api/tickets/{ticketId}/qr-token")
    public ResponseEntity<Map<String, Object>> getDynamicQrToken(@PathVariable Long ticketId) {
        String randomToken = UUID.randomUUID().toString();
        String redisKey = "qr:token:" + randomToken;

        // Redis에 30초 수명으로 저장
        RBucket<Long> bucket = redissonClient.getBucket(redisKey);
        bucket.set(ticketId, Duration.ofSeconds(30));

        return ResponseEntity.ok(Map.of(
                "token", "STAGEON:DYNAMIC:" + randomToken,
                "expiresIn", 30
        ));
    }

    // 3. 스캐너가 QR을 찍었을 때 검증 및 사용 처리
    @ResponseBody
    @GetMapping("/api/tickets/validate")
    public ResponseEntity<String> validateQr(@RequestParam String token) {
        String randomToken = token.replace("STAGEON:DYNAMIC:", "");
        String redisKey = "qr:token:" + randomToken;

        RBucket<Long> bucket = redissonClient.getBucket(redisKey);
        Long ticketId = bucket.get();

        if (ticketId == null) {
            return ResponseEntity.badRequest().body("🚨 만료되거나 유효하지 않은 QR입니다.");
        }

        // 검증 성공 즉시 삭제하여 중복 입장(캡처) 방지
        bucket.delete();
        return ResponseEntity.ok("✅ 입장 성공! (티켓 ID: " + ticketId + ")");
    }
}