package kr.co.stageon.payment.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookApiController {

    // 토스페이먼츠가 결제/입금 상태가 변할 때마다 이 주소로 JSON 데이터를 쏴줍니다.
    @PostMapping("/webhook")
    public ResponseEntity<String> handleTossWebhook(@RequestBody String payload) {

        // 1. 토스에서 보낸 데이터가 잘 들어오는지 로그로 먼저 확인합니다.
        log.info("🔔 [토스 웹훅 수신 완료] 데이터: {}", payload);

        // 2. 추후 이 부분에 JSON(payload)을 파싱해서 DB의 결제 상태를 업데이트하는 로직이 추가됩니다.

        // 3. 토스 서버에게 "우리 서버가 에러 없이 잘 받았어!"라고 200 OK 응답을 돌려줍니다.
        return ResponseEntity.ok("ok");
    }
}