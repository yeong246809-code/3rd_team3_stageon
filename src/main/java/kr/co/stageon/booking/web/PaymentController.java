package kr.co.stageon.booking.web;

import kr.co.stageon.booking.dto.PaymentRequest;
import kr.co.stageon.booking.service.BookingQueryService;
import kr.co.stageon.booking.dto.PaymentSummaryDto;
import kr.co.stageon.booking.service.ReservationService;
import kr.co.stageon.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient; // Redis 클라이언트 추가
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final BookingQueryService bookingQueryService;
    private final ReservationService reservationService;
    private final PaymentService paymentService;

    // 💡 이미 프로젝트에 세팅된 RedissonClient를 주입받아 사용!
    private final RedissonClient redissonClient;

    @PostMapping("/process")
    public String processPayment(@ModelAttribute("paymentRequest") PaymentRequest request, Model model) {

        log.info("결제 페이지 진입 - 선점 ID: {}", request.seatHoldId());

        PaymentSummaryDto summary = bookingQueryService.getPaymentSummaryInfo(request.seatHoldId());
        String orderId = "ORDER-" + UUID.randomUUID().toString();

        // 💡 핵심: 세션 대신 Redis Bucket에 결제 요청 데이터 저장 (TTL 30분 설정)
        RBucket<PaymentRequest> bucket = redissonClient.getBucket("payment:request:" + orderId);
        bucket.set(request, Duration.ofMinutes(30)); // 30분 지나면 찌꺼기 없이 자동 삭제됨!

        model.addAttribute("totalAmount", summary.totalAmount());
        model.addAttribute("performanceTitle", summary.performanceTitle());
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
            // 💡 핵심: Redis에서 orderId로 저장해둔 데이터 꺼내오기
            RBucket<PaymentRequest> bucket = redissonClient.getBucket("payment:request:" + orderId);
            PaymentRequest paymentRequest = bucket.get();

            if (paymentRequest == null) {
                throw new IllegalStateException("결제 시간이 초과되었거나 유효하지 않은 요청입니다.");
            }

            // 토스 결제 승인
            paymentService.confirmPayment(paymentKey, orderId, amount);

            // DB 예매 확정 처리
            Long reservationId = reservationService.confirmReservation(paymentRequest, paymentKey, orderId, amount);

            // 💡 처리 완료 후 Redis에서 데이터 명시적 삭제 (메모리 확보)
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