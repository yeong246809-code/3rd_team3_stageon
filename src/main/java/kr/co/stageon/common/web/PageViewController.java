package kr.co.stageon.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 백엔드 기능 연결 전, Thymeleaf 화면과 공통 프래그먼트를 확인하기 위한 뷰 매핑입니다.
 * 화면 간 링크나 API 호출은 연결하지 않습니다.
 */
@Controller
public class PageViewController {

    @GetMapping({"/", "/index"})
    public String home() {
        return "index";
    }

    @GetMapping("/search-results")
    public String searchResults() {
        return "user/search-results";
    }

    @GetMapping("/performance-detail")
    public String performanceDetail() {
        return "user/performance-detail";
    }

    @GetMapping("/schedule-select")
    public String scheduleSelect() {
        return "booking/schedule-select";
    }

    @GetMapping("/queue")
    public String queue() {
        return "booking/queue";
    }

    @GetMapping("/seat-select")
    public String seatSelect() {
        return "booking/seat-select";
    }

    @GetMapping("/order-review")
    public String orderReview() {
        return "booking/order-review";
    }

    @GetMapping("/mock-payment")
    public String mockPayment() {
        return "booking/mock-payment";
    }

    @GetMapping("/booking-complete")
    public String bookingComplete() {
        return "booking/booking-complete";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "auth/signup";
    }

    @GetMapping("/mypage")
    public String mypage() {
        return "user/mypage";
    }

    @GetMapping("/ai")
    public String ai() {
        return "ai/recommend-faq";
    }

    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin/login";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/performance")
    public String adminPerformance() {
        return "admin/performance-form";
    }

    @GetMapping("/admin/venue-seats")
    public String adminVenueSeats() {
        return "admin/venue-seats";
    }

    @GetMapping("/admin/schedules")
    public String adminSchedules() {
        return "admin/schedules";
    }

    @GetMapping("/admin/seat-inventory")
    public String adminSeatInventory() {
        return "admin/seat-inventory";
    }

    @GetMapping("/admin/orders")
    public String adminOrders() {
        return "admin/orders";
    }
}
