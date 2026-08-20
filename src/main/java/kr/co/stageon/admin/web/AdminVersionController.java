package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.VersionFormDto;
import kr.co.stageon.admin.service.AdminVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** "버전 관리(체인지로그)" 화면 라우팅을 담당합니다. */
@Controller
@RequestMapping("/admin/versions")
@RequiredArgsConstructor
public class AdminVersionController {

    private final AdminVersionService adminVersionService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("versions", adminVersionService.getList());
        return "admin/version-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new VersionFormDto());
        return "admin/version-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", adminVersionService.getForm(id));
        return "admin/version-form";
    }

    /** 상세 모달용 JSON 응답입니다. */
    @GetMapping("/{id}/detail")
    @ResponseBody
    public Object detail(@PathVariable Long id) {
        return adminVersionService.getDetail(id);
    }

    @PostMapping
    public String create(@ModelAttribute VersionFormDto form, RedirectAttributes ra) {
        try {
            adminVersionService.create(form);
            ra.addFlashAttribute("message", "버전 이력이 등록되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/versions";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute VersionFormDto form, RedirectAttributes ra) {
        try {
            adminVersionService.update(id, form);
            ra.addFlashAttribute("message", "버전 이력이 수정되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/versions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminVersionService.delete(id);
            ra.addFlashAttribute("message", "버전 이력이 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/versions";
    }
}