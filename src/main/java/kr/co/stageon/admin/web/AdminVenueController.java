package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.SeatBulkFormDto;
import kr.co.stageon.admin.dto.SeatGradeFormDto;
import kr.co.stageon.admin.dto.SeatMapItemDto;
import kr.co.stageon.admin.dto.VenueFormDto;
import kr.co.stageon.admin.dto.VenueHallFormDto;
import kr.co.stageon.admin.service.AdminVenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * "/admin/venues" 공연장·좌석 관리 화면 전용 컨트롤러입니다.
 * 공연장 신규 등록 기능은 제공하지 않습니다(공연장은 DB 더미 데이터만 사용). 수정·삭제만 가능합니다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/venues")
public class AdminVenueController {

    private final AdminVenueService adminVenueService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("dashboard", adminVenueService.getDashboard());
        return "admin/venue-seats";
    }

    @GetMapping("/{venueId}/edit")
    public String editForm(@PathVariable Long venueId, Model model) {
        model.addAttribute("form", adminVenueService.getVenueForm(venueId));
        return "admin/venue-form";
    }

    @PostMapping("/{venueId}/edit")
    public String update(@PathVariable Long venueId, @ModelAttribute("form") VenueFormDto form) {
        adminVenueService.updateVenue(venueId, form);
        return "redirect:/admin/venues";
    }

    @PostMapping("/{venueId}/delete")
    public String deleteVenue(@PathVariable Long venueId, RedirectAttributes ra) {
        try {
            adminVenueService.deleteVenue(venueId);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/venues";
    }

    @GetMapping("/{venueId}/halls/new")
    public String newHallForm(@PathVariable Long venueId, Model model) {
        model.addAttribute("venue", adminVenueService.getVenueOrThrow(venueId));
        model.addAttribute("form", new VenueHallFormDto());
        return "admin/venue-hall-form";
    }

    @PostMapping("/{venueId}/halls")
    public String createHall(@PathVariable Long venueId, @ModelAttribute("form") VenueHallFormDto form) {
        adminVenueService.createHall(venueId, form);
        return "redirect:/admin/venues";
    }

    /** 홀 상세 페이지입니다. 등급 관리·좌석 생성·홀 삭제 등 홀 관련 관리 기능의 허브입니다. */
    @GetMapping("/halls/{hallId}")
    public String hallDetail(@PathVariable Long hallId, Model model) {
        model.addAttribute("hall", adminVenueService.getHallOrThrow(hallId));
        model.addAttribute("grades", adminVenueService.getHallGrades(hallId));
        return "admin/venue-hall-detail";
    }

    /** "좌석 보기" 모달용 좌석 목록 JSON API입니다. 좌석도가 없으면 빈 배열을 반환합니다. */
    @GetMapping("/halls/{hallId}/seats/map")
    @ResponseBody
    public List<SeatMapItemDto> hallSeatMap(@PathVariable Long hallId) {
        return adminVenueService.getHallSeatMap(hallId);
    }

    @PostMapping("/halls/{hallId}/delete")
    public String deleteHall(@PathVariable Long hallId, RedirectAttributes ra) {
        try {
            adminVenueService.deleteHall(hallId);
            return "redirect:/admin/venues";
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/venues/halls/" + hallId;
        }
    }

    @GetMapping("/halls/{hallId}/grades/new")
    public String newGradeForm(@PathVariable Long hallId, Model model) {
        model.addAttribute("hall", adminVenueService.getHallOrThrow(hallId));
        model.addAttribute("form", new SeatGradeFormDto());
        model.addAttribute("grades", adminVenueService.getHallGrades(hallId));
        return "admin/seat-grade-form";
    }

    @PostMapping("/halls/{hallId}/grades")
    public String createGrade(@PathVariable Long hallId, @ModelAttribute("form") SeatGradeFormDto form) {
        adminVenueService.addGrade(hallId, form);
        return "redirect:/admin/venues/halls/" + hallId + "/grades/new";
    }

    /** 등급 삭제. 배치된 좌석이 있으면 삭제하지 않고 경고 메시지를 화면에 표시합니다(500 방지). */
    @PostMapping("/halls/{hallId}/grades/{gradeId}/delete")
    public String deleteGrade(@PathVariable Long hallId, @PathVariable Long gradeId, RedirectAttributes ra) {
        try {
            adminVenueService.deleteGrade(gradeId);
            ra.addFlashAttribute("message", "등급이 삭제되었습니다.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/venues/halls/" + hallId + "/grades/new";
    }

    /** 등급에 배치된 좌석을 전부 삭제합니다. 삭제 후에는 등급 삭제가 가능해집니다. */
    @PostMapping("/halls/{hallId}/grades/{gradeId}/seats/delete")
    public String deleteGradeSeats(@PathVariable Long hallId, @PathVariable Long gradeId, RedirectAttributes ra) {
        try {
            int count = adminVenueService.deleteSeatsByGrade(gradeId);
            ra.addFlashAttribute("message", count + "개 좌석이 삭제되었습니다. 이제 등급을 삭제할 수 있습니다.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/venues/halls/" + hallId + "/grades/new";
    }

    /**
     * 홀 상세 페이지 "좌석 관리" 모달에서 체크박스로 선택한 좌석만 삭제합니다.
     * 페이지 이동 없이 JSON으로 삭제 결과를 반환합니다.
     */
    @PostMapping("/halls/{hallId}/seats/delete-bulk")
    @ResponseBody
    public java.util.Map<String, Object> deleteSeatsBulk(@PathVariable Long hallId,
                                                         @org.springframework.web.bind.annotation.RequestBody List<Long> seatIds) {
        try {
            int deleted = adminVenueService.deleteSeatsByIds(seatIds);
            return java.util.Map.of("deleted", deleted);
        } catch (IllegalStateException e) {
            return java.util.Map.of("deleted", 0, "error", e.getMessage());
        }
    }

    /**
     * 선택한 좌석 중 이미 회차에 배정되어 삭제가 막힌 좌석이 "어느 공연·회차"에 배정되어 있는지 조회합니다.
     * 삭제 실패(error) 응답을 받은 뒤 모달에서 후속 조회로 호출합니다.
     */
    @PostMapping("/halls/{hallId}/seats/schedule-info")
    @ResponseBody
    public List<kr.co.stageon.admin.dto.ScheduleAssignmentInfoDto> getScheduleAssignments(
            @PathVariable Long hallId,
            @org.springframework.web.bind.annotation.RequestBody List<Long> seatIds) {
        return adminVenueService.getScheduleAssignments(seatIds);
    }

    @GetMapping("/halls/{hallId}/seats/bulk-new")
    public String newBulkSeatForm(@PathVariable Long hallId, Model model) {
        model.addAttribute("hall", adminVenueService.getHallOrThrow(hallId));
        model.addAttribute("grades", adminVenueService.getHallGrades(hallId));
        model.addAttribute("form", new SeatBulkFormDto());
        return "admin/seat-bulk-form";
    }

    @PostMapping("/halls/{hallId}/seats/bulk")
    public String createBulkSeats(@PathVariable Long hallId, @ModelAttribute("form") SeatBulkFormDto form) {
        adminVenueService.bulkCreateSeats(hallId, form);
        return "redirect:/admin/venues/halls/" + hallId + "/seats/bulk-new";
    }
}