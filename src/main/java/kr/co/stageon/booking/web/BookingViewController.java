package kr.co.stageon.booking.web;

import kr.co.stageon.booking.dto.ReservationDetailResponse;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.booking.service.BookingLimitService;
import kr.co.stageon.booking.service.BookingQueryService;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import kr.co.stageon.queue.config.WaitingQueueProperties;
import kr.co.stageon.queue.service.RedisWaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestParam;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import java.time.Duration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/** 회차 선택 이후 예매 단계 화면을 순서대로 연결합니다. */
@Controller
@RequiredArgsConstructor
public class BookingViewController {

    private final BookingQueryService bookingQueryService;
    private final ScheduleSeatRepository scheduleSeatRepository;

    private final PerformanceScheduleRepository performanceScheduleRepository;
    private final MemberRepository memberRepository;
    private final RedisWaitingQueueService waitingQueueService;
    private final BookingLimitService bookingLimitService;

    private final RedissonClient redissonClient;

    /*@GetMapping({"/booking/queue", "/queue"})
    public String queue(@RequestParam(required = false) Long scheduleId, Model model) {
        model.addAttribute("scheduleId", scheduleId);
        return "booking/queue";
    }*/

    @GetMapping({"/booking/seats", "/seat-select"})
    public String seats(@RequestParam(required = false) Long scheduleId,
                         @CookieValue(name = WaitingQueueProperties.COOKIE_NAME, required = false) String queueToken,
                         @AuthenticationPrincipal UserDetails userDetails, // 💡 Security 컨텍스트에서 로그인 유저 정보 주입
                        Model model) {

        model.addAttribute("scheduleId", scheduleId);

        if (scheduleId == null) {
            model.addAttribute("groupedSeats", Collections.emptyMap());
        } else {
            Member member = memberRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("로그인 정보가 유효하지 않습니다."));

            if (!waitingQueueService.hasValidAdmission(scheduleId, member.getId(), queueToken)) {
                return "redirect:/booking/queue?scheduleId=" + scheduleId;
            }

            model.addAttribute("groupedSeats", bookingQueryService.findGroupedSeats(scheduleId));
            model.addAttribute("seatGrades", bookingQueryService.findSeatGrades(scheduleId));

            BookingQueryService.ScheduleSummaryResponse summary = bookingQueryService.findScheduleSummary(scheduleId);

            if (summary != null) {
                model.addAttribute("performanceTitle", summary.performanceTitle());
                model.addAttribute("scheduleTime", summary.scheduleTime());
            }

            // ==============================================================
            // 🚨 로그인한 실제 회원의 남은 예매 가능 수량 계산
            // ==============================================================

            // 1. 로그인 정보에서 회원 조회 후 실제 ID 추출
            Long memberId = member.getId();

            // 2. 선택한 회차의 한도와 공연 전체에서 이미 사용한 매수를 비교합니다.
            var schedule = performanceScheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new IllegalArgumentException("공연 회차를 찾을 수 없습니다."));
            int maxTickets = schedule.getMaxTicketsPerMember() == null
                    ? 4
                    : schedule.getMaxTicketsPerMember();
            int remainingTickets = bookingLimitService.remainingTickets(
                    memberId,
                    schedule.getPerformance().getId(),
                    maxTickets
            );

            // 6. 화면(HTML의 Javascript)으로 값 전달
            model.addAttribute("maxTickets", maxTickets);
            model.addAttribute("remainingTickets", remainingTickets);
        }

        String tabToken = java.util.UUID.randomUUID().toString();

        // 2. Redis에 "유저 이메일 + 스케줄 ID" 조합으로 토큰 저장 (유효시간 30분)
        RBucket<String> tabBucket = redissonClient.getBucket("active_tab:" + userDetails.getUsername() + ":" + scheduleId);
        tabBucket.set(tabToken, Duration.ofMinutes(30));

        // 3. HTML 화면으로 토큰 전달
        model.addAttribute("tabToken", tabToken);

        return "booking/seat-select";
    }

    /*@GetMapping({"/booking/order", "/order-review"})
    public String order(@RequestParam(required = false) Long reservationId, Model model) {
        addReservation(reservationId, model);
        return "booking/order-review";
    }*/

    @GetMapping({"/booking/payment", "/mock-payment"})
    public String payment(@RequestParam(required = false) Long reservationId, Model model) {
        addReservation(reservationId, model);
        return "booking/mock-payment";
    }

    /*@GetMapping({"/booking/complete", "/booking-complete"})
    public String bookingComplete(@RequestParam Long reservationId, Model model) {

        // 1. 예약 ID로 상세 정보 조회 (기존에 만들어두신 DTO 활용)
        ReservationDetailResponse reservation = bookingQueryService.getReservationDetail(reservationId);

        // 2. HTML(Thymeleaf)에서 쓸 수 있게 Model에 담기
        model.addAttribute("reservation", reservation);

        // 3. templates/booking/booking-complete.html 반환
        return "booking/booking-complete";
    }*/

    @GetMapping("/booking/complete")
    public String bookingComplete(@RequestParam("reservationId") Long reservationId, Model model) {

        // 1. 서비스에 세팅해둔 1원짜리 가짜(Mock) 데이터를 가져옵니다.
        ReservationDetailResponse reservation = bookingQueryService.getReservationDetail(reservationId);

        // 2. HTML 화면에서 쓸 수 있도록 모델에 담아줍니다.
        model.addAttribute("reservation", reservation);

        // 3. 완료 페이지 렌더링
        return "booking/booking-complete";
    }

    private void addReservation(Long reservationId, Model model) {
        model.addAttribute("reservationId", reservationId);
        if (reservationId != null) {
            bookingQueryService.findReservation(reservationId)
                    .ifPresent(reservation -> model.addAttribute("reservation", reservation));
        }
    }
}
