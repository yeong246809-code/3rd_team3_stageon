package kr.co.stageon.booking.web;

import kr.co.stageon.booking.dto.PaymentRequest;
import kr.co.stageon.booking.service.BookingQueryService;
import kr.co.stageon.booking.dto.PaymentSummaryDto;
import kr.co.stageon.booking.service.PortOneService;
import kr.co.stageon.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Slf4j
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final BookingQueryService bookingQueryService;
    private final PortOneService portOneService;
    private final ReservationService reservationService;

    // 1. 주문 확인(order-review) -> 모의 결제(포트원) 페이지 렌더링
    @PostMapping("/process")
    public String processPayment(@ModelAttribute("paymentRequest") PaymentRequest request, Model model) {
        log.info("결제 페이지 진입 - 선점 ID: {}, 이름: {}, 연락처: {}, 수령방법: {}",
                request.seatHoldId(), request.bookerName(), request.bookerPhone(), request.receiveMethod());

        // DB에서 SeatHoldId로 실제 총 결제 금액과 공연 제목을 계산하여 가져옵니다.
        PaymentSummaryDto summary = bookingQueryService.getPaymentSummaryInfo(request.seatHoldId());

        BigDecimal totalAmount = new BigDecimal("1");
        model.addAttribute("totalAmount", totalAmount);
        //model.addAttribute("totalAmount", summary.totalAmount());
        model.addAttribute("performanceTitle", summary.performanceTitle());

        // 포트원 결제창이 대기 중인 모의 결제 화면으로 이동
        return "booking/mock-payment";
    }

    // 2. 포트원 결제 완료 후 -> 사후 검증 및 최종 예매 확정 처리
    @PostMapping("/complete")
    public String completePayment(
            @RequestParam("impUid") String impUid,           // 포트원이 발급한 결제 고유번호
            @RequestParam("merchantUid") String merchantUid, // 우리가 생성한 주문번호
            @ModelAttribute PaymentRequest paymentRequest,
            RedirectAttributes redirectAttributes) {

        log.info("결제 사후 검증 시작 - impUid: {}, merchantUid: {}", impUid, merchantUid);

        try {
            // 1. DB에서 실제 기대하는 총 결제 금액 다시 조회 (클라이언트 조작 방지)
            PaymentSummaryDto summary = bookingQueryService.getPaymentSummaryInfo(paymentRequest.seatHoldId());
            BigDecimal expectedAmount = new BigDecimal("1");
            //BigDecimal expectedAmount = summary.totalAmount();

            // 2. 포트원 서버와 통신하여 실제 결제된 금액이 맞는지 사후 검증
            boolean isVerified = portOneService.verifyPayment(impUid, expectedAmount);

            if (!isVerified) {
                throw new IllegalStateException("결제 검증 실패: 결제 금액이 불일치하거나 미결제 상태입니다.");
            }

            // 3. 검증 성공 시 DB에 최종 예매 확정 처리 (Reservation, ReservationSeat 생성)
            Long reservationId = reservationService.confirmReservation(paymentRequest, impUid, merchantUid, expectedAmount);

            log.info("예매 확정 완료 - reservationId: {}", reservationId);

            // 4. 예매 완료 페이지로 리다이렉트
            return "redirect:/booking/complete?reservationId=" + reservationId;

        } catch (Exception e) {
            log.error("결제 승인 및 예매 확정 중 오류 발생: ", e);

            // 사용자에게 보여줄 에러 메시지를 담아서 메인 화면(또는 이전 화면)으로 돌려보냅니다.
            redirectAttributes.addFlashAttribute("errorMessage", "결제 처리 중 문제가 발생했습니다: " + e.getMessage());

            return "redirect:/";
        }
    }
}