package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.ScheduleBulkFormDto;
import kr.co.stageon.admin.dto.ScheduleEditFormDto;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/** AD07 "일정·회차 관리" 화면 라우팅을 담당합니다. */
@Controller
@RequiredArgsConstructor
public class AdminScheduleController {

    private final AdminScheduleService adminScheduleService;
    private final PerformanceRepository performanceRepository;

    @GetMapping("/admin/schedules")
    public String schedules(@RequestParam(required = false) Long performanceId,
                            @RequestParam(required = false) String month,
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
        model.addAttribute("overview", adminScheduleService.getOverview(false, false));
        model.addAttribute("allItems", adminScheduleService.getOverview(true, false));
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

    /** 회차 추가 달력에서 선택한 홀의 점유 날짜(다른 공연이 이미 등록된 날짜)를 조회합니다. */
    @GetMapping("/admin/schedules/halls/{hallId}/occupied-dates")
    @ResponseBody
    public Set<String> getHallOccupiedDates(@PathVariable Long hallId,
                                            @RequestParam Long performanceId) {
        return adminScheduleService.getHallOccupiedDates(hallId, performanceId);
    }

    /** 달력에서 선택한 여러 날짜에 회차를 한 번에 생성합니다. */
    @PostMapping("/admin/schedules/bulk")
    public String createBulk(@ModelAttribute ScheduleBulkFormDto form, RedirectAttributes ra) {
        try {
            var result = adminScheduleService.createBulkSchedules(form);
            String msg = result.getCreatedCount() + "건의 회차가 생성되었습니다.";
            if (result.getSkippedCount() > 0) {
                msg += " (" + result.getSkippedCount() + "건은 날짜 형식 오류로 건너뜀)";
            }
            ra.addFlashAttribute("message", msg);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/schedules";
    }

    /** 회차 번호·일정·매수를 수정합니다. 필터를 유지한 채로 목록으로 되돌아갑니다. */
    @PostMapping("/admin/schedules/{id}/edit")
    public String edit(@PathVariable Long id,
                       @ModelAttribute ScheduleEditFormDto form,
                       @RequestParam(required = false) Long performanceId,
                       @RequestParam(required = false) String month,
                       RedirectAttributes ra) {
        try {
            adminScheduleService.updateSchedule(id, form);
            ra.addFlashAttribute("message", "회차 정보가 수정되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return buildRedirect(performanceId, month);
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
        return buildRedirect(performanceId, month);
    }

    private String buildRedirect(Long performanceId, String month) {
        StringBuilder redirect = new StringBuilder("redirect:/admin/schedules?");
        if (performanceId != null) redirect.append("performanceId=").append(performanceId).append("&");
        if (month != null && !month.isBlank()) redirect.append("month=").append(month);
        return redirect.toString();
    }
}