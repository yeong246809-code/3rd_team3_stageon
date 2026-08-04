package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.PerformanceFormDto;
import kr.co.stageon.admin.service.AdminDashboardService;
import kr.co.stageon.admin.service.AdminPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** 관리자 시안의 메뉴별 화면 경로를 한 곳에서 관리합니다. */
@Controller
@RequiredArgsConstructor
public class AdminViewController {

    private final AdminDashboardService adminDashboardService;
    private final AdminPerformanceService adminPerformanceService;

    @GetMapping("/admin/login")
    public String login() { return "admin/login"; }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", adminDashboardService.getDashboardStats());
        return "admin/dashboard";
    }

    @GetMapping("/admin/performances")
    public String performances(Model model) {
        model.addAttribute("performances", adminPerformanceService.getList());
        return "admin/performance-list";
    }

    @GetMapping("/admin/performances/new")
    public String performanceNew(Model model) {
        model.addAttribute("form", new PerformanceFormDto());
        model.addAttribute("readOnly", false);
        return "admin/performance-form";
    }

    @GetMapping("/admin/performances/{id}/edit")
    public String performanceEdit(@PathVariable Long id, Model model) {
        model.addAttribute("form", adminPerformanceService.getForm(id));
        model.addAttribute("readOnly", false);
        return "admin/performance-form";
    }

    /** 정보 보기 전용 화면입니다. 폼과 동일하지만 모든 입력이 읽기전용으로 잠깁니다. */
    @GetMapping("/admin/performances/{id}/view")
    public String performanceView(@PathVariable Long id, Model model) {
        model.addAttribute("form", adminPerformanceService.getForm(id));
        model.addAttribute("readOnly", true);
        return "admin/performance-form";
    }

    // "/admin/venues" 관련 GET·POST 매핑은 AdminVenueController로 이동했습니다.
    // "/admin/schedules" 매핑은 AdminScheduleController로 이동했습니다.

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