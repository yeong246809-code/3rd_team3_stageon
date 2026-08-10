package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.SeatInventoryScheduleOptionDto;
import kr.co.stageon.admin.service.AdminSeatInventoryService;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** AD08 "좌석 재고·선점 현황" 화면 라우팅을 담당합니다. */
@Controller
@RequiredArgsConstructor
public class AdminSeatInventoryController {

    private final AdminSeatInventoryService adminSeatInventoryService;
    private final PerformanceRepository performanceRepository;

    @GetMapping("/admin/seat-inventory")
    public String seatInventory(@RequestParam(required = false) Long performanceId,
                                @RequestParam(required = false) Long scheduleId,
                                Model model) {
        List<Performance> performances = performanceRepository.findAll();
        Long selectedPerformanceId = (performanceId != null) ? performanceId
                : (performances.isEmpty() ? null : performances.get(0).getId());

        List<SeatInventoryScheduleOptionDto> scheduleOptions = (selectedPerformanceId != null)
                ? adminSeatInventoryService.getScheduleOptions(selectedPerformanceId)
                : List.of();

        Long selectedScheduleId = (scheduleId != null) ? scheduleId
                : (scheduleOptions.isEmpty() ? null : scheduleOptions.get(0).id());

        model.addAttribute("performances", performances);
        model.addAttribute("scheduleOptions", scheduleOptions);
        model.addAttribute("selectedPerformanceId", selectedPerformanceId);
        model.addAttribute("selectedScheduleId", selectedScheduleId);
        model.addAttribute("stats", adminSeatInventoryService.getStats(selectedScheduleId));
        model.addAttribute("grades", adminSeatInventoryService.getGradeBreakdown(selectedScheduleId));
        model.addAttribute("sections", adminSeatInventoryService.getSeatMap(selectedScheduleId));
        model.addAttribute("activeHolds", adminSeatInventoryService.getActiveHolds(selectedScheduleId));
        return "admin/seat-inventory";
    }
}