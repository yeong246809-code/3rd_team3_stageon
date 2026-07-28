package kr.co.stageon.admin.web;

import kr.co.stageon.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 관리자 시안의 메뉴별 화면 경로를 한 곳에서 관리합니다. */
@Controller
@RequiredArgsConstructor
public class AdminViewController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/admin/login")
    public String login() { return "admin/login"; }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", adminDashboardService.getDashboardStats());
        return "admin/dashboard";
    }

    @GetMapping("/admin/performances")
    public String performances() { return "admin/performance-form"; }

    @GetMapping("/admin/venues")
    public String venues() { return "admin/venue-seats"; }

    @GetMapping("/admin/schedules")
    public String schedules() { return "admin/schedules"; }

    @GetMapping("/admin/seat-inventory")
    public String seatInventory() { return "admin/seat-inventory"; }

    @GetMapping("/admin/orders")
    public String orders() { return "admin/orders"; }

    /** 원격 시안에서 사용하던 단수 주소를 새 목록 주소로 넘깁니다. */
    @GetMapping("/admin/performance")
    public String legacyPerformance() { return "redirect:/admin/performances"; }

    @GetMapping("/admin/venue-seats")
    public String legacyVenueSeats() { return "redirect:/admin/venues"; }
}