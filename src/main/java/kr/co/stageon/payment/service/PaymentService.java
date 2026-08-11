package kr.co.stageon.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PaymentService {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 토스페이먼츠 최종 결제 승인 (Confirm) API를 호출합니다.
     */
    public String confirmPayment(String paymentKey, String orderId, BigDecimal amount) {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        String secretKey = "test_sk_nRQoOaPz8LxZAbvJq0Wz8y47BMw6";

        String authBasic = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + authBasic);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentKey", paymentKey);
        payload.put("orderId", orderId);
        payload.put("amount", amount);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        try {
            // 1. 토스 API 최종 승인 요청
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.tosspayments.com/v1/payments/confirm",
                    requestEntity,
                    String.class
            );

            // 2. JSON 응답 파싱 및 method(결제수단) 추출
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            // 3. "카드", "가상계좌" 등의 문자열을 반환하여 Controller로 넘겨줌
            return jsonNode.get("method").asText();

        } catch (Exception e) {
            throw new RuntimeException("결제 승인 중 오류가 발생했습니다. (토스 API 호출 실패)", e);
        }
    }
}