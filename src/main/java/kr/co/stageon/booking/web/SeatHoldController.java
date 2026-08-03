package kr.co.stageon.booking.web;

import kr.co.stageon.booking.dto.SeatHoldRequest;
import kr.co.stageon.booking.facade.SeatHoldRedissonFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/booking/seats")
@RequiredArgsConstructor
public class SeatHoldController {

    private final SeatHoldRedissonFacade seatHoldRedissonFacade;

    @PostMapping("/hold")
    public String holdSeats(SeatHoldRequest request, RedirectAttributes redirectAttributes) {
        try {
            // 1. 분산 락을 통한 선점 로직 실행
            seatHoldRedissonFacade.holdSeats(request);

            // 2. 성공 시 결제/주문서 페이지로 이동 (예약 ID 등은 상황에 맞게 전달)
            return "redirect:/booking/order?scheduleId=" + request.scheduleId();

        } catch (IllegalStateException | IllegalArgumentException e) {
            // 3. 튕겨났을 경우 (누가 먼저 채감, 매수 초과 등) 기존 좌석 선택 페이지로 리다이렉트
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/booking/seats?scheduleId=" + request.scheduleId();
        }
    }
}