package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.SeatInventoryScheduleOptionDto;
import kr.co.stageon.admin.service.AdminSeatInventoryService;
import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    /** 회차의 좌석도에 있는 물리 좌석 중 아직 생성되지 않은 좌석을 일괄 생성합니다. */
    @PostMapping("/admin/seat-inventory/generate")
    public String generate(@RequestParam Long scheduleId,
                           @RequestParam(required = false) Long performanceId,
                           RedirectAttributes ra) {
        try {
            int count = adminSeatInventoryService.generateMissingScheduleSeats(scheduleId);
            ra.addFlashAttribute("message", count > 0 ? count + "개 좌석이 생성되었습니다." : "이미 모든 좌석이 생성되어 있습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return buildRedirect(performanceId, scheduleId);
    }

    /** 좌석 맵에서 관리자가 좌석 하나의 상태를 직접 변경합니다. */
    @PostMapping("/admin/seat-inventory/seats/status")
    public String changeSeatStatus(@RequestParam Long seatId,
                                   @RequestParam String status,
                                   @RequestParam(required = false) Long performanceId,
                                   @RequestParam(required = false) Long scheduleId,
                                   RedirectAttributes ra) {
        try {
            adminSeatInventoryService.updateSeatStatus(seatId, ScheduleSeat.Status.valueOf(status));
            ra.addFlashAttribute("message", "좌석 상태가 변경되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return buildRedirect(performanceId, scheduleId);
    }

    private String buildRedirect(Long performanceId, Long scheduleId) {
        StringBuilder redirect = new StringBuilder("redirect:/admin/seat-inventory?");
        if (performanceId != null) redirect.append("performanceId=").append(performanceId).append("&");
        if (scheduleId != null) redirect.append("scheduleId=").append(scheduleId);
        return redirect.toString();
    }
}