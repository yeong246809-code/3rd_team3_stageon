package kr.co.stageon.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortOneService {

    @Value("${portone.api-key}")
    private String apiKey;

    @Value("${portone.api-secret}")
    private String apiSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    // 1. 포트원 API 접근을 위한 엑세스 토큰 발급
    private String getAccessToken() {
        String url = "https://api.iamport.kr/users/getToken";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"imp_key\":\"%s\", \"imp_secret\":\"%s\"}", apiKey, apiSecret);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseData = (Map<String, Object>) response.getBody().get("response");
            return (String) responseData.get("access_token");
        }
        throw new IllegalStateException("포트원 엑세스 토큰 발급에 실패했습니다.");
    }

    // 2. imp_uid로 결제 정보 조회 후 금액 검증
    public boolean verifyPayment(String impUid, BigDecimal expectedAmount) {
        String token = getAccessToken();
        String url = "https://api.iamport.kr/payments/" + impUid + "?include_sandbox=true";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token); // Authorization: Bearer {token}

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseData = (Map<String, Object>) response.getBody().get("response");

            // 포트원 서버에 기록된 실제 결제 금액
            Integer amount = (Integer) responseData.get("amount");
            String status = (String) responseData.get("status");

            log.info("결제 검증 - impUid: {}, 포트원금액: {}, DB예상금액: {}, 상태: {}",
                    impUid, amount, expectedAmount, status);

            // 결제 상태가 'paid(결제완료)'이고, 금액이 일치하는지 확인
            return "paid".equals(status) && expectedAmount.compareTo(new BigDecimal(amount)) == 0;
        }
        return false;
    }
}