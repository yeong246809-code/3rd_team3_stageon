package kr.co.stageon.payment.controller;

import kr.co.stageon.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentWebhookApiController {

    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleTossWebhook(@RequestBody Map<String, Object> payload) {
        // 1. 무조건 일단 찍어봅니다! (데이터가 어떻게 오는지 눈으로 확인)
        log.info("🔔 [웹훅 수신 원본 데이터]: {}", payload);

        String status = null;
        String orderId = null;

        // 2. 토스가 'data' 껍데기에 싸서 보낸 경우 (일반 상태 변경 이벤트)
        if (payload.containsKey("data")) {
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            status = (String) data.get("status");
            orderId = (String) data.get("orderId");
        }
        // 3. 토스가 껍데기 없이 평탄하게 보낸 경우 (가상계좌 입금 완료 이벤트)
        else {
            status = (String) payload.get("status");
            orderId = (String) payload.get("orderId");
        }

        log.info("🔔 [웹훅 파싱 결과] 주문번호: {}, 상태: {}", orderId, status);

        // 4. 입금 완료(DONE) 상태일 때만 DB 업데이트 로직 실행
        if ("DONE".equals(status)) {
            paymentService.completePayment(orderId);
            log.info("✅ 결제 상태 업데이트 완료 (주문번호: {})", orderId);
        } else if ("CANCELED".equals(status)) {
            paymentService.cancelPayment(orderId);
            log.info("🚫 결제 기한만료/취소 업데이트 완료 (주문번호: {})", orderId);
        }

        // 토스 서버에게 "잘 처리했어" 라고 알려줌 (이걸 안 주면 토스가 계속 재시도합니다)
        return ResponseEntity.ok("ok");
    }
}