package kr.co.stageon.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class PaymentService {

    // 💡 방금 캡처해서 보여주신 그 '시크릿 키'를 여기에 넣습니다!
    // (실무에서는 application.yml에 빼두지만, 지금은 테스트를 위해 직접 적어둡니다)
    private final String TOSS_SECRET_KEY = "test_sk_nRQoOaPz8LxZAbvJq0Wz8y47BMw6";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 토스페이먼츠 최종 결제 승인 (Confirm) API를 호출합니다.
     */
    public void confirmPayment(String paymentKey, String orderId, BigDecimal amount) {
        log.info("토스 결제 승인 API 호출 시작 - paymentKey: {}, orderId: {}, amount: {}", paymentKey, orderId, amount);

        // 1. 토스 API 호출을 위한 헤더 설정 (Basic Auth)
        HttpHeaders headers = new HttpHeaders();
        // 시크릿 키 뒤에 콜론(:)을 붙여서 Base64 인코딩하는 것이 토스의 필수 규칙입니다.
        String encodedAuth = Base64.getEncoder().encodeToString((TOSS_SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
        headers.setBasicAuth(encodedAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. 요청 바디 데이터 세팅
        Map<String, Object> requestBody = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 3. 토스페이먼츠로 승인(Confirm) POST 요청 쏘기
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.tosspayments.com/v1/payments/confirm",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("토스 결제 승인 성공! 응답: {}", response.getBody());

        } catch (Exception e) {
            log.error("토스 결제 승인 실패: ", e);
            throw new RuntimeException("결제 승인에 실패했습니다. (잔액 부족, 한도 초과 등)");
        }
    }
}