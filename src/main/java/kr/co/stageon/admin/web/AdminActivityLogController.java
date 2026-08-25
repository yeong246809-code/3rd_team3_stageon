package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.AdminActivityLogRowDto;
import kr.co.stageon.admin.dto.AdminActivityLogSearchDto;
import kr.co.stageon.admin.service.AdminActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * 감사 로그(AD11) 조회 화면.
 * 이 컨트롤러는 자체 라우팅만 소유하며, 실제 로그 "기록"은
 * kr.co.stageon.admin.log.AdminActivityLogAspect가 다른 서비스 메서드에 붙은
 * @AdminLoggable을 감지해 자동으로 처리합니다. (이 컨트롤러는 조회 전용)
 *
 * 관리자 계정이 1개뿐인 프로젝트 특성상 관리자 이메일 검색 조건은 제거함(2026-08-25).
 */
@Controller
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AdminActivityLogController {

    private static final int PAGE_SIZE = 5;

    private final AdminActivityLogService adminActivityLogService;

    @GetMapping
    public String list(
            @RequestParam(value = "actionType", required = false) String actionType,
            @RequestParam(value = "targetEntity", required = false) String targetEntity,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model
    ) {
        AdminActivityLogSearchDto searchDto =
                new AdminActivityLogSearchDto(actionType, targetEntity, dateFrom, dateTo);

        Page<AdminActivityLogRowDto> result = adminActivityLogService.search(searchDto, page, PAGE_SIZE);

        model.addAttribute("logs", result);
        model.addAttribute("search", searchDto);
        return "admin/audit-logs";
    }
}