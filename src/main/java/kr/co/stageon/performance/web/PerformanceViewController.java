package kr.co.stageon.performance.web;

import kr.co.stageon.performance.service.PerformanceQueryService;
import kr.co.stageon.performance.support.PerformanceGenres;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** 공연 검색·상세·회차 선택 Thymeleaf 화면을 연결합니다. */
@Controller
@RequiredArgsConstructor
public class PerformanceViewController {

    private final PerformanceQueryService performanceQueryService;

    @GetMapping({"/performances", "/search-results"})
    public String search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String genre,
            Model model
    ) {
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("selectedGenre", genre == null ? "" : genre.trim());
        model.addAttribute("genreOptions", PerformanceGenres.options());
        model.addAttribute("performances", performanceQueryService.findPerformances(keyword, genre));
        return "user/search-results";
    }

    @GetMapping("/performances/{performanceId}")
    public String detail(@PathVariable Long performanceId, Model model) {
        performanceQueryService.findPerformance(performanceId)
                .ifPresent(performance -> model.addAttribute("performance", performance));
        model.addAttribute("schedules", performanceQueryService.findSchedules(performanceId));
        return "user/performance-detail";
    }

    /** 기존 정적 시안 주소를 유지하기 위한 호환 경로입니다. */
    @GetMapping("/performance-detail")
    public String legacyDetail() {
        return "user/performance-detail";
    }

    @GetMapping("/performances/{performanceId}/schedules")
    public String schedules(
            @PathVariable Long performanceId,
            Model model
    ) {
        var performance = performanceQueryService
                .findPerformance(performanceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("공연을 찾을 수 없습니다.")
                );

        model.addAttribute("performance", performance);
        model.addAttribute("performanceId", performanceId);
        model.addAttribute(
                "schedules",
                performanceQueryService.findSchedules(performanceId)
        );

        return "booking/schedule-select";
    }

    @GetMapping("/schedule-select")
    public String legacySchedules() {
        return "booking/schedule-select";
    }
}
