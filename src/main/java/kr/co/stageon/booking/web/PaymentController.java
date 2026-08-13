package kr.co.stageon.booking.web;

import com.fasterxml.jackson.databind.JsonNode;
import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.domain.SeatHoldItem;
import kr.co.stageon.booking.dto.PaymentRequest;
import kr.co.stageon.booking.service.BookingQueryService;
import kr.co.stageon.booking.service.ReservationService;
import kr.co.stageon.payment.domain.Payment; // 💡 결제 수단(PayMethod) Enum을 위해 추가
import kr.co.stageon.payment.service.PaymentService;
import kr.co.stageon.booking.repository.SeatHoldItemRepository;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final BookingQueryService bookingQueryService;
    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final SeatHoldItemRepository seatHoldItemRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final RedissonClient redissonClient;

    @PostMapping("/process")
    public String processPayment(@ModelAttribute("paymentRequest") PaymentRequest request, Model model) {
        log.info("결제 페이지 진입 - 선점 ID: {}", request.seatHoldId());

        SeatHold seatHold = seatHoldRepository.findById(request.seatHoldId())
                .orElseThrow(() -> new IllegalArgumentException("선점 내역을 찾을 수 없습니다."));
        List<SeatHoldItem> holdItems = seatHoldItemRepository.findBySeatHoldIdOrderByIdAsc(seatHold.getId());

        BigDecimal totalPrice = holdItems.stream()
                .map(item -> item.getScheduleSeat().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 숫자형 14자리 짧은 주문번호(orderId) 생성
        String dateStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomNum = (int)(Math.random() * 900000) + 100000;
        String orderId = dateStr + randomNum;

        RBucket<PaymentRequest> bucket = redissonClient.getBucket("payment:request:" + orderId);
        bucket.set(request, Duration.ofMinutes(30));

        model.addAttribute("holdItems", holdItems);
        model.addAttribute("totalCount", holdItems.size());
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("performanceTitle", seatHold.getSchedule().getPerformance().getTitle());
        model.addAttribute("scheduleTime", seatHold.getSchedule().getStartsAt());
        model.addAttribute("orderId", orderId);

        return "booking/mock-payment";
    }

    @GetMapping("/success")
    public String completePayment(
            @RequestParam("paymentKey") String paymentKey,
            @RequestParam("orderId") String orderId,
            @RequestParam("amount") BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        try {
            RBucket<PaymentRequest> bucket = redissonClient.getBucket("payment:request:" + orderId);
            PaymentRequest paymentRequest = bucket.get();

            if (paymentRequest == null) {
                throw new IllegalStateException("결제 시간이 초과되었거나 유효하지 않은 요청입니다.");
            }

            // 💡 1. 토스 API 호출 -> JsonNode 덩어리를 받습니다.
            JsonNode tossResponse = paymentService.confirmPayment(paymentKey, orderId, amount);

            // 💡 2. 글자 매핑 에러 방지: 혹시 모를 공백이나 줄바꿈을 완벽히 제거합니다.
            String rawMethod = tossResponse.hasNonNull("method") ? tossResponse.get("method").asText() : "";
            String tossMethod = rawMethod.replaceAll("\\s+", ""); // 모든 공백 제거
            String tossStatus = tossResponse.hasNonNull("status") ? tossResponse.get("status").asText().trim() : "";

            log.info("🔥 [디버깅] 정제된 결제수단: [{}], 상태: [{}]", tossMethod, tossStatus);

            // 💡 3. switch 문 대신 contains(포함 여부)로 안전하게 매핑
            Payment.PayMethod payMethod = Payment.PayMethod.CARD; // 기본값

            if (tossMethod.contains("가상계좌")) {
                payMethod = Payment.PayMethod.VBANK;
            } else if (tossMethod.contains("계좌이체")) {
                payMethod = Payment.PayMethod.BANK;
            } else if (tossMethod.contains("휴대폰")) {
                payMethod = Payment.PayMethod.MOBILE;
            }

            log.info("🔥 [디버깅] 최종 변환된 Enum: {}", payMethod);

            // 💡 4. 가상계좌 정보 추출 로직
            String vbankNum = null;
            String vbankName = null;
            LocalDateTime vbankDueDate = null;

            if (tossResponse.hasNonNull("virtualAccount")) {
                JsonNode vaNode = tossResponse.get("virtualAccount");
                vbankNum = vaNode.get("accountNumber").asText();
                vbankName = vaNode.get("bankCode").asText(); // 토스 은행코드 (예: 06)

                // "2026-08-19T16:43:13+09:00" 형태의 문자열을 LocalDateTime으로 변환
                String dueDateStr = vaNode.get("dueDate").asText();
                vbankDueDate = OffsetDateTime.parse(dueDateStr).toLocalDateTime();
            }

            // 💡 5. 토스 상태값에 따라 DB에 저장될 상태 결정
            Payment.Status paymentStatus = "WAITING_FOR_DEPOSIT".equals(tossStatus)
                    ? Payment.Status.READY
                    : Payment.Status.SUCCESS;

            // 💡 6. DB 확정 처리 시 새롭게 추출한 값들도 함께 넘겨줍니다.
            Long reservationId = reservationService.confirmReservation(
                    paymentRequest, paymentKey, orderId, amount, payMethod,
                    paymentStatus, vbankNum, vbankName, vbankDueDate
            );

            bucket.delete();

            return "redirect:/booking/complete?reservationId=" + reservationId;

        } catch (Exception e) {
            log.error("결제 승인 중 오류 발생: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "결제 처리 중 문제가 발생했습니다.");
            return "redirect:/";
        }
    }

    @GetMapping("/fail")
    public String failPayment(
            @RequestParam(value = "message", defaultValue = "결제에 실패했습니다.") String message,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errorMessage", message);
        return "redirect:/";
    }
}