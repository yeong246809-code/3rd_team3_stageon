package kr.co.stageon.admin.web;

import kr.co.stageon.admin.dto.PerformanceFormDto;
import kr.co.stageon.admin.dto.SeatGradeSummaryDto;
import kr.co.stageon.admin.dto.SeatMapItemDto;
import kr.co.stageon.admin.service.AdminPerformanceService;
import kr.co.stageon.admin.service.AdminVenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @PostMapping("/admin/performances")
    public String create(@ModelAttribute PerformanceFormDto form,
                         @RequestParam(defaultValue = "false") boolean draft) {
        adminPerformanceService.create(form, draft);
        return "redirect:/admin/performances";
    }

    @PostMapping("/admin/performances/{id}")
    public String update(@PathVariable Long id, @ModelAttribute PerformanceFormDto form,
                         @RequestParam(defaultValue = "false") boolean draft) {
        adminPerformanceService.update(id, form, draft);
        return "redirect:/admin/performances";
    }

    /** 목록 화면의 삭제 확인 모달에서 fetch로 호출합니다. 회차가 남아있으면 409로 차단됩니다. */
    @DeleteMapping("/admin/performances/{id}")
    @ResponseBody
    public ResponseEntity<String> delete(@PathVariable Long id) {
        try {
            String title = adminPerformanceService.delete(id);
            return ResponseEntity.ok(title + " 공연이 삭제되었습니다.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
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

    /** 좌석/가격 설정 모달에서 등급명을 즉시 변경(DB 반영)합니다. 색상·좌석수·정렬순서는 유지됩니다. */
    @PostMapping("/admin/performances/grades/{gradeId}/rename")
    @ResponseBody
    public void renameGrade(@PathVariable Long gradeId, @RequestParam String name) {
        adminVenueService.renameGrade(gradeId, name);
    }

}