package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.ScheduleListItemDto;
import kr.co.stageon.admin.service.AdminScheduleService;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
                            Model model) {
        List<Performance> performances = performanceRepository.findAll();
        Long selectedPerformanceId = performanceId != null
                ? performanceId
                : (performances.isEmpty() ? null : performances.get(0).getId());
        YearMonth selectedMonth = (month != null && !month.isBlank()) ? YearMonth.parse(month) : YearMonth.now();

        List<ScheduleListItemDto> scheduleList = selectedPerformanceId != null
                ? adminScheduleService.getListByPerformanceAndMonth(selectedPerformanceId, selectedMonth)
                : List.of();

        model.addAttribute("stats", adminScheduleService.getStats());
        model.addAttribute("overview", adminScheduleService.getOverview());
        model.addAttribute("performances", performances);
        model.addAttribute("selectedPerformanceId", selectedPerformanceId);
        model.addAttribute("selectedMonth", selectedMonth.toString());
        model.addAttribute("scheduleList", scheduleList);
        return "admin/schedules";
    }
}