package kr.co.stageon.performance.web;

import kr.co.stageon.booking.service.BookingLimitService;
import kr.co.stageon.member.repository.MemberRepository;
import kr.co.stageon.performance.repository.PerformanceRepository;
import kr.co.stageon.performance.service.ScheduleSelectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

/**
 * [남수아 담당]
 * 날짜·회차 선택 화면 전용 Controller입니다.
 *
 * 기존 PerformanceViewController는 수정하지 않습니다.
 */
@Controller
@RequiredArgsConstructor
public class ScheduleSelectionController {

    private final PerformanceRepository performanceRepository;
    private final ScheduleSelectionService scheduleSelectionService;
    private final MemberRepository memberRepository;
    private final BookingLimitService bookingLimitService;

    /**
     * 공연의 날짜와 회차를 선택하는 화면입니다.
     *
     * starts_at을 기준으로 달력과 공연 시간을 구성하고,
     * 좌석 등급별 잔여석을 함께 전달합니다.
     *
     * @param performanceId 공연 번호
     * @param model Thymeleaf에 전달할 데이터
     * @return 날짜·회차 선택 화면
     */
    @GetMapping("/booking/performances/{performanceId}/schedules")
    public String scheduleSelection(
            @PathVariable Long performanceId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        var performance = performanceRepository
                .findById(performanceId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "공연을 찾을 수 없습니다."
                        )
                );

        var scheduleSelections = scheduleSelectionService
                .findScheduleSelections(performanceId);

        var member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("로그인 정보가 유효하지 않습니다."));

        int maxTickets = scheduleSelections.stream()
                .map(selection -> selection.maxTicketsPerMember())
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(4);

        if (!scheduleSelections.isEmpty()
                && bookingLimitService.remainingTickets(
                        member.getId(),
                        performanceId,
                        maxTickets
                ) == 0) {
            redirectAttributes.addAttribute("bookingLimitReached", true);
            return "redirect:/performances/" + performanceId;
        }

        // 화면 상단의 공연 정보
        model.addAttribute(
                "performance",
                performance
        );

        // 다음 예매 흐름으로 전달할 공연 번호
        model.addAttribute(
                "performanceId",
                performanceId
        );

        // 회차 정보와 좌석 등급별 잔여석
        model.addAttribute(
                "scheduleSelections",
                scheduleSelections
        );

        return "booking/schedule-select";
    }
}
