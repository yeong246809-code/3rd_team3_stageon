package kr.co.stageon.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            // 🚨 핵심 포인트: String이나 JsonNode 대신 byte[]로 받아옵니다. (한글 깨짐 원천 차단)
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                    "https://api.tosspayments.com/v1/payments/confirm",
                    requestEntity,
                    byte[].class
            );

            // 💡 순수 바이트 데이터를 JsonNode로 변환하면 스프링 에러도 없고 한글도 완벽하게 나옵니다.
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

    /**
     * 가상계좌 미입금 취소 또는 일반 결제 취소 웹훅 수신 시 결제 상태를 업데이트합니다.
     */
    @Transactional
    public void cancelPayment(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문번호입니다: " + orderId));

        if (payment.getStatus() == Payment.Status.CANCELED) {
            log.info("이미 취소 처리된 결제건입니다. orderId: {}", orderId);
            return;
        }

        // 💡 가상계좌 미입금 취소(READY)일 경우 전용 메서드 호출
        if (payment.getStatus() == Payment.Status.READY) {
            payment.markUnpaidCanceled();

            // 🚨 여기에 예약 취소 및 좌석 원상복구(해제) 로직을 꼭 넣어주세요!
            // 예: reservationService.cancelReservation(payment.getReservation().getId());
        } else {
            log.warn("미입금 취소 대상이 아닙니다. 현재 상태: {}", payment.getStatus());
        }
    }

    /**
     * 토스페이먼츠 결제 취소 API 호출 (부분/전체 취소 공통)
     */
    public void cancelTossPayment(Payment payment, BigDecimal cancelAmount, String cancelReason,
                                  String refundBank, String refundAccountNumber, String refundHolderName) {
        RestTemplate restTemplate = new RestTemplate();
        String secretKey = "test_sk_nRQoOaPz8LxZAbvJq0Wz8y47BMw6";
        String authBasic = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + authBasic);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("cancelReason", cancelReason);
        payload.put("cancelAmount", cancelAmount);

        // [핵심 수정] 가상계좌(VBANK) 결제 건이라면 사용자가 입력한 진짜 계좌 정보를 넣습니다.
        if (payment.getPayMethod() == Payment.PayMethod.VBANK && payment.getStatus() == Payment.Status.SUCCESS) {

            if (refundBank == null || refundBank.isEmpty() || refundAccountNumber == null || refundAccountNumber.isEmpty()) {
                throw new IllegalArgumentException("입금 완료된 가상계좌 취소 시 환불 계좌 정보는 필수입니다.");
            }

            Map<String, String> refundAccount = new HashMap<>();
            refundAccount.put("bank", refundBank);
            refundAccount.put("accountNumber", refundAccountNumber);
            refundAccount.put("holderName", refundHolderName);

            payload.put("refundReceiveAccount", refundAccount);
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.tosspayments.com/v1/payments/" + payment.getPaymentKey() + "/cancel",
                    requestEntity,
                    String.class
            );
            log.info("✅ 토스페이먼츠 환불 계좌 반영 결제 취소 성공: {}", response.getBody());
        } catch (Exception e) {
            log.error("토스 결제 취소 API 호출 실패", e);
            throw new RuntimeException("결제망 취소 요청 중 오류가 발생했습니다.");
        }
    }
}