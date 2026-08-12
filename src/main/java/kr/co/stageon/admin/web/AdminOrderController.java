package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.AdminOrderSearchCondition;
import kr.co.stageon.admin.service.AdminOrderService;
import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.payment.domain.Payment;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

/** AD09 "예매·주문 조회" 화면 라우팅을 담당합니다. */
@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private static final int PAGE_SIZE = 10;

    private final AdminOrderService adminOrderService;
    private final PerformanceRepository performanceRepository;

    @GetMapping
    public String orders(@RequestParam(required = false) Long performanceId,
                         @RequestParam(required = false) Long scheduleId,
                         @RequestParam(required = false) Reservation.Status status,
                         @RequestParam(required = false) Payment.Status paymentStatus,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
                         @RequestParam(defaultValue = "1") int page,
                         Model model) {

        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;

        AdminOrderSearchCondition condition = new AdminOrderSearchCondition(
                performanceId, scheduleId, status, paymentStatus, keyword, from, to);

        Page<?> result = adminOrderService.search(condition,
                PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE));

        List<Performance> performances = performanceRepository.findAll();

        model.addAttribute("orders", result);
        model.addAttribute("performances", performances);
        model.addAttribute("stats", adminOrderService.getStats());
        model.addAttribute("reservationStatuses", Reservation.Status.values());
        model.addAttribute("paymentStatuses", Payment.Status.values());

        model.addAttribute("selectedPerformanceId", performanceId);
        model.addAttribute("selectedScheduleId", scheduleId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPaymentStatus", paymentStatus);
        model.addAttribute("keyword", keyword);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("currentPage", page);

        return "admin/orders";
    }

    /** 상세 모달용 JSON 응답입니다. */
    @GetMapping("/{id}/detail")
    @ResponseBody
    public Object detail(@PathVariable Long id) {
        return adminOrderService.getDetail(id);
    }

    /** 관리자 강제취소 처리입니다. */
    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @RequestParam String reason,
                         @RequestParam(required = false) String redirectQuery,
                         RedirectAttributes ra) {
        try {
            adminOrderService.cancelReservation(id, reason);
            ra.addFlashAttribute("message", "예매가 취소 처리되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        String query = (redirectQuery != null && !redirectQuery.isBlank()) ? "?" + redirectQuery : "";
        return "redirect:/admin/orders" + query;
    }

    /** 관리자 재예매 처리입니다. 취소된 예매를 동일 회원·동일 좌석으로 다시 예매 확정합니다. */
    @PostMapping("/{id}/rebook")
    public String rebook(@PathVariable Long id,
                         @RequestParam(required = false) String redirectQuery,
                         RedirectAttributes ra) {
        try {
            adminOrderService.rebookReservation(id);
            ra.addFlashAttribute("message", "재예매가 완료되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        String query = (redirectQuery != null && !redirectQuery.isBlank()) ? "?" + redirectQuery : "";
        return "redirect:/admin/orders" + query;
    }
}