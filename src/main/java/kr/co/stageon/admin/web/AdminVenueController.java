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
    public String deleteVenue(@PathVariable Long venueId) {
        adminVenueService.deleteVenue(venueId);
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
    public String deleteHall(@PathVariable Long hallId) {
        adminVenueService.deleteHall(hallId);
        return "redirect:/admin/venues";
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

    @PostMapping("/halls/{hallId}/grades/{gradeId}/delete")
    public String deleteGrade(@PathVariable Long hallId, @PathVariable Long gradeId) {
        adminVenueService.deleteGrade(gradeId);
        return "redirect:/admin/venues/halls/" + hallId + "/grades/new";
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