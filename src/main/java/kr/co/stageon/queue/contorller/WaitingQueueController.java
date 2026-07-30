package kr.co.stageon.queue.contorller;

import kr.co.stageon.queue.dto.QueueInfoResponse;
import kr.co.stageon.queue.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/booking") // 공통 상위 경로 지정
@RequiredArgsConstructor
public class WaitingQueueController {

    private final WaitingQueueService waitingQueueService;

    /**
     * 대기열 페이지 진입
     * 진입 URL 예시: http://localhost:8080/booking/queue?performanceId=1&scheduleId=1&token=hash_token_0001
     */
    @GetMapping("/queue")
    public String queuePage(
            @RequestParam(value = "performanceId", required = false, defaultValue = "1") Long performanceId,
            @RequestParam("scheduleId") Long scheduleId,
            @RequestParam("token") String queueToken,
            Model model
    ) {
        // 1. 서비스 계층에서 대기열 순위 및 상태 정보 조회
        QueueInfoResponse queueInfo = waitingQueueService.getQueueInfo(performanceId, scheduleId, queueToken);

        // 2. Thymeleaf 뷰(queue.html)로 데이터 전달
        model.addAttribute("queueInfo", queueInfo);

        return "/booking/queue";
    }
}