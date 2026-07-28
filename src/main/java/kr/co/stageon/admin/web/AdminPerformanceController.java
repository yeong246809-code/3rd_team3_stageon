package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.PerformanceFormDto;
import kr.co.stageon.admin.service.AdminPerformanceService;
import kr.co.stageon.common.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 공연 등록·수정 폼의 실제 저장 처리를 담당합니다. */
@Controller
@RequiredArgsConstructor
public class AdminPerformanceController {

    private final AdminPerformanceService adminPerformanceService;
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

    /** 새 파일이 업로드된 경우에만 posterUrl을 교체하고, 없으면 기존 값(hidden field)을 그대로 둡니다. */
    private void applyUploadedPoster(PerformanceFormDto form) {
        String savedUrl = fileStorageService.save(form.getPosterFile());
        if (savedUrl != null) {
            form.setPosterUrl(savedUrl);
        }
    }
}