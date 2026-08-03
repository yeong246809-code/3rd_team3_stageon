package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.SeatGradeFormDto;
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
import org.springframework.web.bind.annotation.RequestParam;

/** "/admin/venues" 공연장·좌석 관리 화면 전용 컨트롤러입니다. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/venues")
public class AdminVenueController {

    private final AdminVenueService adminVenueService;

    @GetMapping
    public String list(@RequestParam(required = false) Long hallId, Model model) {
        model.addAttribute("dashboard", adminVenueService.getDashboard());
        model.addAttribute("selectedHallId", hallId);
        model.addAttribute("grades", adminVenueService.getHallGrades(hallId));
        return "admin/venue-seats";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new VenueFormDto());
        return "admin/venue-form";
    }

    @PostMapping
    public String create(@ModelAttribute("form") VenueFormDto form) {
        adminVenueService.createVenue(form);
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
}