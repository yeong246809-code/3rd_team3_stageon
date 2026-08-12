package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.PerformanceFormDto;
import kr.co.stageon.admin.service.AdminDashboardService;
import kr.co.stageon.admin.service.AdminPerformanceService;
import kr.co.stageon.admin.service.AdminVenueService;
import kr.co.stageon.performance.domain.Performance;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** 관리자 시안의 메뉴별 화면 경로를 한 곳에서 관리합니다. */
@Controller
@RequiredArgsConstructor
public class AdminViewController {

    /** 공연 목록 한 페이지에 보여줄 개수입니다. */
    private static final int PERFORMANCE_PAGE_SIZE = 5;

    private final AdminDashboardService adminDashboardService;
    private final AdminPerformanceService adminPerformanceService;
    private final AdminVenueService adminVenueService;

    @GetMapping("/admin/login")
    public String login() { return "admin/login"; }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", adminDashboardService.getDashboardStats());
        return "admin/dashboard";
    }

    @GetMapping("/admin/performances")
    public String performances(
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        int pageIndex = Math.max(page - 1, 0); // 화면은 1페이지부터, Pageable은 0부터 시작
        Page<Performance> performancePage =
                adminPerformanceService.getList(pageIndex, PERFORMANCE_PAGE_SIZE);
        model.addAttribute("performances", performancePage);
        return "admin/performance-list";
    }

    @GetMapping("/admin/performances/new")
    public String performanceNew(Model model) {
        model.addAttribute("form", new PerformanceFormDto());
        model.addAttribute("readOnly", false);
        model.addAttribute("halls", getHallOptions());
        return "admin/performance-form";
    }

    @GetMapping("/admin/performances/{id}/edit")
    public String performanceEdit(@PathVariable Long id, Model model) {
        model.addAttribute("form", adminPerformanceService.getForm(id));
        model.addAttribute("readOnly", false);
        model.addAttribute("halls", getHallOptions());
        return "admin/performance-form";
    }

    /** 정보 보기 전용 화면입니다. 폼과 동일하지만 모든 입력이 읽기전용으로 잠깁니다.
     *  (목록 화면에서는 이제 팝업으로 포스터만 확인하지만, 직접 URL 접근을 위해 남겨둡니다.) */
    @GetMapping("/admin/performances/{id}/view")
    public String performanceView(@PathVariable Long id, Model model) {
        model.addAttribute("form", adminPerformanceService.getForm(id));
        model.addAttribute("readOnly", true);
        model.addAttribute("halls", getHallOptions());
        return "admin/performance-form";
    }

    /** 공연 등록·수정 폼의 "공연장 선택" 드롭다운용 홀 목록입니다. 홀이 없는 공연장(row.hallId == null)은 제외합니다. */
    private java.util.List<kr.co.stageon.admin.dto.VenueStructureRowDto> getHallOptions() {
        return adminVenueService.getDashboard().getRows().stream()
                .filter(row -> row.getHallId() != null)
                .toList();
    }

    // "/admin/venues" 관련 GET·POST 매핑은 AdminVenueController로 이동했습니다.
    // "/admin/schedules" 매핑은 AdminScheduleController로 이동했습니다.
    // "/admin/seat-inventory" 매핑은 AdminSeatInventoryController로 이동했습니다.
    // "/admin/orders" 매핑은 AdminOrderController로 이동했습니다.

    /** 원격 시안에서 사용하던 단수 주소를 새 목록 주소로 넘깁니다. */
    @GetMapping("/admin/performance")
    public String legacyPerformance() { return "redirect:/admin/performances"; }

    @GetMapping("/admin/venue-seats")
    public String legacyVenueSeats() { return "redirect:/admin/venues"; }
}