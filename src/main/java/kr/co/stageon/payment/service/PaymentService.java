package kr.co.stageon.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 토스페이먼츠 최종 결제 승인 (Confirm) API를 호출합니다.
     */
    public JsonNode confirmPayment(String paymentKey, String orderId, BigDecimal amount) {
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
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.tosspayments.com/v1/payments/confirm",
                    requestEntity,
                    String.class
            );

            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {
            throw new RuntimeException("결제 승인 중 오류가 발생했습니다.", e);
        }
    }

    /**
    가상계좌 입금 완료 웹훅 수신 시 결제 상태를 업데이트합니다.
     */
    @Transactional
    public Payment createVirtualAccountPayment(Reservation reservation, String paymentKey,
                                               String orderId, BigDecimal amount,
                                               String vbankNum, String vbankName, LocalDateTime vbankDueDate) {

        Payment payment = Payment.builder()
                .reservation(reservation)
                .paymentKey(paymentKey)
                .orderId(orderId)
                .provider(Payment.Provider.TOSSPAYMENTS)
                .payMethod(Payment.PayMethod.VBANK) // 결제수단: 가상계좌
                .amount(amount)
                .status(Payment.Status.READY)       // 최초 상태: 결제 대기
                .vbankNum(vbankNum)
                .vbankName(vbankName)
                .vbankDueDate(vbankDueDate)
                .requestedAt(LocalDateTime.now())
                .build();

        return paymentRepository.save(payment);
    }

    @Transactional
    public void completePayment(String orderId) {
        // 1. 주문번호로 DB에서 결제 내역 조회
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문번호입니다: " + orderId));

        // 2. 이미 결제가 완료된 건인지 방어 로직 (멱등성 보장)
        if (payment.getStatus() == Payment.Status.SUCCESS) {
            log.info("이미 처리된 결제건입니다. orderId: {}", orderId);
            return;
        }

        // 3. 상태를 SUCCESS로 변경
        payment.complete();

        log.info("✅ 가상계좌 입금 확인 및 DB 업데이트 완료 - 주문번호: {}", orderId);
    }
}