package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.ScheduleFormDto;
import kr.co.stageon.admin.dto.ScheduleListItemDto;
import kr.co.stageon.admin.service.AdminScheduleService;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import kr.co.stageon.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;
import java.util.List;

/** AD07 "일정·회차 관리" 화면 라우팅을 담당합니다. */
@Controller
@RequiredArgsConstructor
public class AdminScheduleController {

    private final AdminScheduleService adminScheduleService;
    private final PerformanceRepository performanceRepository;

    @GetMapping("/admin/schedules")
    public String schedules(@RequestParam(required = false) Long performanceId,
                            @RequestParam(required = false) String month,
                            @RequestParam(defaultValue = "false") boolean all,
                            @RequestParam(defaultValue = "false") boolean conflictOnly,
                            Model model) {
        List<Performance> performances = performanceRepository.findAll();
        Long selectedPerformanceId = (performanceId != null) ? performanceId
                : (performances.isEmpty() ? null : performances.get(0).getId());

        YearMonth selectedMonth;
        try {
            selectedMonth = (month != null && !month.isBlank()) ? YearMonth.parse(month) : YearMonth.now();
        } catch (Exception e) {
            selectedMonth = YearMonth.now();
        }

        List<ScheduleListItemDto> scheduleList = selectedPerformanceId != null
                ? adminScheduleService.getListByPerformanceAndMonth(selectedPerformanceId, selectedMonth)
                : List.of();

        model.addAttribute("stats", adminScheduleService.getStats());
        model.addAttribute("overview", adminScheduleService.getOverview(all, conflictOnly));
        model.addAttribute("overviewAll", all);
        model.addAttribute("overviewConflictOnly", conflictOnly);
        model.addAttribute("performances", performances);
        model.addAttribute("hallOptions", adminScheduleService.getHallOptions());
        model.addAttribute("selectedPerformanceId", selectedPerformanceId);
        model.addAttribute("selectedMonth", selectedMonth.toString());
        model.addAttribute("scheduleList", scheduleList);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ScheduleFormDto());
        }
        return "admin/schedules";
    }

    @PostMapping("/admin/schedules")
    public String create(@ModelAttribute("form") ScheduleFormDto form, RedirectAttributes ra) {
        try {
            adminScheduleService.createSchedule(form);
            ra.addFlashAttribute("message", "회차가 등록되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/schedules";
    }

    /** 회차 판매 상태를 변경합니다(판매 시작/판매 종료/취소). 필터를 유지한 채로 목록으로 되돌아갑니다. */
    @PostMapping("/admin/schedules/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam String status,
                               @RequestParam(required = false) Long performanceId,
                               @RequestParam(required = false) String month,
                               RedirectAttributes ra) {
        try {
            adminScheduleService.changeStatus(id, PerformanceSchedule.Status.valueOf(status));
            ra.addFlashAttribute("message", "회차 상태가 변경되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        StringBuilder redirect = new StringBuilder("redirect:/admin/schedules?");
        if (performanceId != null) redirect.append("performanceId=").append(performanceId).append("&");
        if (month != null && !month.isBlank()) redirect.append("month=").append(month);
        return redirect.toString();
    }
}