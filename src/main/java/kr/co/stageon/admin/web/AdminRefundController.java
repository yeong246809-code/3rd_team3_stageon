package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.AdminRefundSearchCondition;
import kr.co.stageon.admin.service.AdminRefundService;
import kr.co.stageon.payment.domain.Refund;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** "환불 관리" 화면 라우팅을 담당합니다. */
@Controller
@RequestMapping("/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    private static final int PAGE_SIZE = 5;

    private final AdminRefundService adminRefundService;

    @GetMapping
    public String list(@RequestParam(required = false) Refund.Status status,
                       @RequestParam(required = false) Refund.Category category,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {

        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;

        AdminRefundSearchCondition condition = new AdminRefundSearchCondition(status, category, keyword, from, to);

        Page<?> result = adminRefundService.search(condition, PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE));

        model.addAttribute("refunds", result);
        model.addAttribute("statuses", Refund.Status.values());
        model.addAttribute("categories", Refund.Category.values());

        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("keyword", keyword);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("currentPage", page);

        return "admin/refund-list";
    }

    /** 상세 모달용 JSON 응답입니다. */
    @GetMapping("/{id}/detail")
    @ResponseBody
    public Object detail(@PathVariable Long id) {
        return adminRefundService.getDetail(id);
    }

    /** 수동환불 모달 - 회원 검색 자동완성입니다. */
    @GetMapping("/members/search")
    @ResponseBody
    public Object searchMembers(@RequestParam String keyword) {
        return adminRefundService.searchMembers(keyword);
    }

    /** 수동환불 모달 - 선택한 회원의 환불 가능 결제 목록입니다. */
    @GetMapping("/members/{memberId}/payments")
    @ResponseBody
    public Object memberPayments(@PathVariable Long memberId) {
        return adminRefundService.getMemberRefundablePayments(memberId);
    }

    /** 관리자 수동 환불(예매 취소 없이 결제 건 단위 부분/전액 환불) 처리입니다. */
    @PostMapping("/manual")
    public String manualRefund(@RequestParam Long paymentId,
                               @RequestParam BigDecimal amount,
                               @RequestParam String reason,
                               @RequestParam(required = false) String redirectQuery,
                               RedirectAttributes ra) {
        try {
            adminRefundService.manualRefund(paymentId, amount, reason);
            ra.addFlashAttribute("message", "환불이 처리되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        String query = (redirectQuery != null && !redirectQuery.isBlank()) ? "?" + redirectQuery : "";
        return "redirect:/admin/refunds" + query;
    }
}