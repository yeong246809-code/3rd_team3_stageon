package kr.co.stageon.queue.controller;

import kr.co.stageon.queue.dto.QueueInfoResponse;
import kr.co.stageon.queue.service.RedisWaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/booking")
@RequiredArgsConstructor
public class WaitingQueueController {

    private final RedisWaitingQueueService queueService;

    @GetMapping("/queue")
    public String queuePage(
            @RequestParam(value = "performanceId", required = false, defaultValue = "1") Long performanceId,
            @RequestParam("scheduleId") Long scheduleId,
            @RequestParam("token") String queueToken,
            Model model
    ) {
        QueueInfoResponse queueInfo = queueService.getQueueInfo(performanceId, scheduleId, queueToken);

       model.addAttribute("queueInfo", queueInfo);

        return "/booking/queue";
    }
}