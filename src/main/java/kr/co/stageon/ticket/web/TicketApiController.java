package kr.co.stageon.ticket.web;

import kr.co.stageon.booking.dto.MyTicketResponse;
import kr.co.stageon.booking.service.MyTicketQueryService;
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
    private final MyTicketQueryService myTicketQueryService;

    // 1. 관리자 스캐너 화면 띄우기
    @GetMapping("/admin/scanner")
    public String scannerPage() {
        return "admin/scanner";
    }

    // 2. 관객에게 30초짜리 동적 QR 토큰 발급
    @ResponseBody
    @GetMapping("/api/tickets/{ticketId}/qr-token")
    public ResponseEntity<Map<String, Object>> getDynamicQrToken(@PathVariable Long ticketId) {
        String randomToken = UUID.randomUUID().toString();
        String redisKey = "qr:token:" + randomToken;

        RBucket<Long> bucket = redissonClient.getBucket(redisKey);
        bucket.set(ticketId, Duration.ofSeconds(30));

        return ResponseEntity.ok(Map.of(
                "token", "STAGEON:DYNAMIC:" + randomToken,
                "expiresIn", 30
        ));
    }

    // 3. 스캐너가 QR을 찍었을 때 검증 및 예매 정보 반환
    @ResponseBody
    @GetMapping("/api/tickets/validate")
    public ResponseEntity<?> validateQr(@RequestParam String token) {
        String randomToken = token.replace("STAGEON:DYNAMIC:", "");
        String redisKey = "qr:token:" + randomToken;

        RBucket<Long> bucket = redissonClient.getBucket(redisKey);
        Long seatId = bucket.get();

        if (seatId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "🚨 만료되거나 유효하지 않은 QR입니다."));
        }

        MyTicketResponse ticketInfo = myTicketQueryService.getTicketBySeatId(seatId);

        // 검증 성공 즉시 삭제하여 중복 입장(캡처) 방지
        bucket.delete();

        return ResponseEntity.ok(ticketInfo);
    }

    // 4. 직원이 '입장 완료' 버튼을 눌렀을 때 호출
    @ResponseBody
    @PostMapping("/api/tickets/{seatId}/enter")
    public ResponseEntity<String> enterTicket(@PathVariable Long seatId) {
        myTicketQueryService.processTicketEntry(seatId);
        return ResponseEntity.ok("✅ 정상적으로 입장 처리되었습니다.");
    }
}