package kr.co.stageon.queue.controller;

import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import kr.co.stageon.queue.config.WaitingQueueProperties;
import kr.co.stageon.queue.dto.QueueInfoResponse;
import kr.co.stageon.queue.service.RedisWaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WaitingQueueStatusApiController {

    private final RedisWaitingQueueService queueService;
    private final MemberRepository memberRepository;
    private final PerformanceScheduleRepository scheduleRepository;

    @GetMapping("/api/waiting-queue/status")
    public QueueInfoResponse status(
            @RequestParam Long scheduleId,
            @CookieValue(name = WaitingQueueProperties.COOKIE_NAME, required = false) String queueToken,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("로그인 회원 정보를 찾을 수 없습니다."));
        PerformanceSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("선택한 공연 회차를 찾을 수 없습니다."));

        return queueService.getQueueInfo(
                schedule.getPerformance().getId(),
                scheduleId,
                member.getId(),
                queueToken
        );
    }
}
