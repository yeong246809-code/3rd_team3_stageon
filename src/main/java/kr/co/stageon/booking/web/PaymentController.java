package kr.co.stageon.booking.web;

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

            // 💡 1. 토스 승인 API를 호출하고, 결과로 '결제 수단(method)'을 반환받습니다.
            String tossMethod = paymentService.confirmPayment(paymentKey, orderId, amount);

            // 💡 2. 토스가 넘겨준 진짜 결제 수단 문자열을 우리 DB의 Enum 값으로 매핑!
            Payment.PayMethod payMethod = switch (tossMethod) {
                case "가상계좌" -> Payment.PayMethod.VBANK;
                case "계좌이체" -> Payment.PayMethod.BANK;
                case "휴대폰" -> Payment.PayMethod.MOBILE;
                default -> Payment.PayMethod.CARD; // 신용카드 및 토스페이/카카오페이 등 간편결제
            };

            // 3. DB 예매 확정 처리 (동적으로 변환된 payMethod 전달)
            Long reservationId = reservationService.confirmReservation(paymentRequest, paymentKey, orderId, amount, payMethod);

            // 4. Redis 정리
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