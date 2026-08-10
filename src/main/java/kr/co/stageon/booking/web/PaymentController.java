package kr.co.stageon.booking.web;

import kr.co.stageon.booking.dto.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;

@Slf4j
@Controller
@RequestMapping("/payment")
public class PaymentController {

    @PostMapping("/process")
    public String processPayment(@ModelAttribute("paymentRequest") PaymentRequest request, Model model) {
        log.info("결제 페이지 진입 - 선점 ID: {}, 이름: {}, 연락처: {}, 수령방법: {}",
                request.seatHoldId(), request.bookerName(), request.bookerPhone(), request.receiveMethod());

        // TODO: 실제로는 SeatHoldId를 통해 DB에서 이 예매의 정확한 총 금액(totalAmount)과 공연 제목(performanceTitle)을 조회해야 합니다.
        // 임시로 테스트용 금액과 제목을 세팅합니다.
        BigDecimal totalAmount = new BigDecimal("162000");
        String performanceTitle = "StageOn 티켓 예매";

        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("performanceTitle", performanceTitle);

        // templates/booking/mock-payment.html 파일을 렌더링
        return "booking/mock-payment";
    }
}