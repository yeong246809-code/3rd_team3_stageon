package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.BannerFormDto;
import kr.co.stageon.admin.service.AdminBannerService;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 배너 관리(메인 히어로 슬라이더) 화면 라우팅을 담당합니다. */
@Controller
@RequestMapping("/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final AdminBannerService adminBannerService;
    private final PerformanceRepository performanceRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("banners", adminBannerService.getList());
        return "admin/banner-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new BannerFormDto());
        model.addAttribute("performances", getPerformanceOptions());
        return "admin/banner-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", adminBannerService.getForm(id));
        model.addAttribute("performances", getPerformanceOptions());
        return "admin/banner-form";
    }

    @PostMapping
    public String create(@ModelAttribute BannerFormDto form, RedirectAttributes ra) {
        try {
            adminBannerService.create(form);
            ra.addFlashAttribute("message", "배너가 등록되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/banners";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute BannerFormDto form, RedirectAttributes ra) {
        try {
            adminBannerService.update(id, form);
            ra.addFlashAttribute("message", "배너가 수정되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/banners";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminBannerService.delete(id);
            ra.addFlashAttribute("message", "배너가 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/banners";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminBannerService.toggleActive(id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/banners";
    }

    @PostMapping("/{id}/move-up")
    public String moveUp(@PathVariable Long id) {
        adminBannerService.moveUp(id);
        return "redirect:/admin/banners";
    }

    @PostMapping("/{id}/move-down")
    public String moveDown(@PathVariable Long id) {
        adminBannerService.moveDown(id);
        return "redirect:/admin/banners";
    }

    private java.util.List<Performance> getPerformanceOptions() {
        return performanceRepository.findAll();
    }
}