package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.PerformanceFormDto;
import kr.co.stageon.admin.dto.SeatGradeSummaryDto;
import kr.co.stageon.admin.dto.SeatMapItemDto;
import kr.co.stageon.admin.service.AdminPerformanceService;
import kr.co.stageon.admin.service.AdminVenueService;
import kr.co.stageon.common.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/** 공연 등록·수정 폼의 실제 저장 처리를 담당합니다. */
@Controller
@RequiredArgsConstructor
public class AdminPerformanceController {

    private final AdminPerformanceService adminPerformanceService;
    private final AdminVenueService adminVenueService;
    private final FileStorageService fileStorageService;

    @PostMapping("/admin/performances")
    public String create(@ModelAttribute PerformanceFormDto form,
                         @RequestParam(defaultValue = "false") boolean draft) {
        applyUploadedPoster(form);
        adminPerformanceService.create(form, draft);
        return "redirect:/admin/performances";
    }

    @PostMapping("/admin/performances/{id}")
    public String update(@PathVariable Long id, @ModelAttribute PerformanceFormDto form,
                         @RequestParam(defaultValue = "false") boolean draft) {
        applyUploadedPoster(form);
        adminPerformanceService.update(id, form, draft);
        return "redirect:/admin/performances";
    }

    /** 좌석/가격 설정 모달에서 선택한 홀의 등급 목록을 조회합니다. */
    @GetMapping("/admin/performances/halls/{hallId}/grades")
    @ResponseBody
    public List<SeatGradeSummaryDto> getHallGrades(@PathVariable Long hallId) {
        return adminVenueService.getHallGrades(hallId);
    }

    /** 좌석/가격 설정 모달에서 보여줄 실제 좌석 배치도(개별 좌석)를 조회합니다. */
    @GetMapping("/admin/performances/halls/{hallId}/seats")
    @ResponseBody
    public List<SeatMapItemDto> getHallSeatMap(@PathVariable Long hallId) {
        return adminVenueService.getHallSeatMap(hallId);
    }

    /** 새 파일이 업로드된 경우에만 posterUrl을 교체하고, 없으면 기존 값(hidden field)을 그대로 둡니다. */
    private void applyUploadedPoster(PerformanceFormDto form) {
        String savedUrl = fileStorageService.save(form.getPosterFile());
        if (savedUrl != null) {
            form.setPosterUrl(savedUrl);
        }
    }
}