package kr.co.stageon.performance.api;

import kr.co.stageon.performance.dto.PerformanceDetailResponse;
import kr.co.stageon.performance.dto.PerformanceSummaryResponse;
import kr.co.stageon.performance.dto.ScheduleResponse;
import kr.co.stageon.performance.service.PerformanceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** 화면과 별도로 사용할 수 있는 공연 조회 REST API입니다. */
@RestController
@RequestMapping("/api/performances")
@RequiredArgsConstructor
public class PerformanceQueryApiController {

    private final PerformanceQueryService performanceQueryService;

    @GetMapping
    public List<PerformanceSummaryResponse> performances(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String genre
    ) {
        return performanceQueryService.findPerformances(keyword, genre);
    }

    @GetMapping("/{performanceId}")
    public PerformanceDetailResponse performance(@PathVariable Long performanceId) {
        return performanceQueryService.findPerformance(performanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공연을 찾을 수 없습니다."));
    }

    @GetMapping("/{performanceId}/schedules")
    public List<ScheduleResponse> schedules(@PathVariable Long performanceId) {
        return performanceQueryService.findSchedules(performanceId);
    }
}
