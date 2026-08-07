package kr.co.stageon.booking.web;

import kr.co.stageon.booking.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    // private final PaymentService paymentService; // 실제 결제 처리 서비스 (필요 시 주입)

    @PostMapping("/process")
    public String processPayment(PaymentRequest request, RedirectAttributes redirectAttributes) {
        log.info("결제 요청 수신 - 선점 ID: {}, 이름: {}, 연락처: {}, 수령방법: {}",
                request.seatHoldId(), request.bookerName(), request.bookerPhone(), request.receiveMethod());

        // TODO: 여기서 Reservation 엔티티 생성(모의 결제 처리) 비즈니스 로직 호출
        // paymentService.processPaymentAndReserve(request);

        // 완료 후 띄워줄 파라미터(예: 생성된 예매 번호)를 넘겨주며 성공 페이지로 리다이렉트
        // redirectAttributes.addAttribute("bookingId", createdReservationId);

        return "redirect:/booking/success";
    }
}