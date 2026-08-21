package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.AdminPerformanceOptionDto;
import kr.co.stageon.admin.dto.AdminSettlementListItemDto;
import kr.co.stageon.admin.dto.AdminSettlementSearchCondition;
import kr.co.stageon.admin.service.AdminSettlementService;
import kr.co.stageon.performance.domain.Performance;
import kr.co.stageon.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** "정산·매출 관리" 화면 라우팅을 담당합니다. */
@Controller
@RequestMapping("/admin/settlements")
@RequiredArgsConstructor
public class AdminSettlementController {

    /** 공연별 매출 목록 한 페이지에 보여줄 개수입니다. */
    private static final int PAGE_SIZE = 5;

    private final AdminSettlementService adminSettlementService;
    private final PerformanceRepository performanceRepository;

    @GetMapping
    public String settlements(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                              @RequestParam(defaultValue = "1") int page,
                              Model model) {

        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;

        AdminSettlementSearchCondition condition = new AdminSettlementSearchCondition(keyword, from, to);

        Page<AdminSettlementListItemDto> result = adminSettlementService.search(condition,
                PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE));

        model.addAttribute("settlements", result);
        model.addAttribute("stats", adminSettlementService.getStats(condition));
        model.addAttribute("keyword", keyword);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("currentPage", page);

        return "admin/settlements";
    }

    /** 상세 모달용 JSON 응답입니다. 목록 화면에서 적용 중인 기간 필터를 그대로 전달받아 회차별 breakdown에도 반영합니다. */
    @GetMapping("/{performanceId}/detail")
    @ResponseBody
    public Object detail(@PathVariable Long performanceId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;
        return adminSettlementService.getDetail(performanceId, new AdminSettlementSearchCondition(null, from, to));
    }

    /** "공연명 검색" 모달용 공연 목록입니다. keyword가 없으면 전체를 반환합니다. */
    @GetMapping("/performance-options")
    @ResponseBody
    public List<AdminPerformanceOptionDto> performanceOptions(@RequestParam(required = false) String keyword) {
        List<Performance> list = (keyword == null || keyword.isBlank())
                ? performanceRepository.findAll()
                : performanceRepository.findByTitleContainingIgnoreCaseOrderByStartDateAsc(keyword);
        return list.stream()
                .map(p -> new AdminPerformanceOptionDto(p.getId(), p.getTitle()))
                .toList();
    }
}