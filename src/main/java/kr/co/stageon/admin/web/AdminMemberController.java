package kr.co.stageon.admin.web;

import jakarta.servlet.http.HttpSession;
import kr.co.stageon.admin.auth.AdminAuthController;
import kr.co.stageon.admin.dto.AdminMemberDetailDto;
import kr.co.stageon.admin.dto.AdminMemberListItemDto;
import kr.co.stageon.admin.service.AdminMemberService;
import kr.co.stageon.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** AD10 "회원 관리" 화면 라우팅을 담당합니다. */
@Controller
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private static final int PAGE_SIZE = 10;

    private final AdminMemberService adminMemberService;

    @GetMapping
    public String members(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Member.Role role,
                          @RequestParam(required = false) Member.Status status,
                          @RequestParam(defaultValue = "1") int page,
                          HttpSession session,
                          Model model) {

        Page<AdminMemberListItemDto> result = adminMemberService.search(
                keyword, role, status, PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE));

        model.addAttribute("members", result);
        model.addAttribute("roles", Member.Role.values());
        model.addAttribute("statuses", Member.Status.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("currentAdminId", session.getAttribute(AdminAuthController.SESSION_KEY_ADMIN));

        return "admin/members";
    }

    /** 상세 모달용 JSON 응답입니다. */
    @GetMapping("/{id}/detail")
    @ResponseBody
    public AdminMemberDetailDto detail(@PathVariable Long id) {
        return adminMemberService.getDetail(id);
    }

    /** 회원 권한 변경입니다. 관리자 본인 계정은 변경할 수 없습니다. */
    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam Member.Role role,
                             @RequestParam(required = false) String redirectQuery,
                             HttpSession session,
                             RedirectAttributes ra) {
        Object loginAdminId = session.getAttribute(AdminAuthController.SESSION_KEY_ADMIN);
        if (loginAdminId != null && loginAdminId.equals(id)) {
            ra.addFlashAttribute("error", "본인 계정의 권한은 변경할 수 없습니다.");
        } else {
            try {
                adminMemberService.changeRole(id, role);
                ra.addFlashAttribute("message", "권한이 변경되었습니다.");
            } catch (Exception e) {
                ra.addFlashAttribute("error", e.getMessage());
            }
        }
        String query = (redirectQuery != null && !redirectQuery.isBlank()) ? "?" + redirectQuery : "";
        return "redirect:/admin/members" + query;
    }

    /** 관리자 메모 저장입니다. */
    @PostMapping("/{id}/memo")
    public String updateMemo(@PathVariable Long id,
                             @RequestParam(required = false) String memo,
                             @RequestParam(required = false) String redirectQuery,
                             RedirectAttributes ra) {
        try {
            adminMemberService.updateMemo(id, memo);
            ra.addFlashAttribute("message", "메모가 저장되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        String query = (redirectQuery != null && !redirectQuery.isBlank()) ? "?" + redirectQuery : "";
        return "redirect:/admin/members" + query;
    }
}