package kr.co.stageon.booking.web;

import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.domain.SeatHoldItem;
import kr.co.stageon.booking.repository.SeatHoldItemRepository;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/booking")
@RequiredArgsConstructor
public class OrderReviewController {

    private final MemberRepository memberRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatHoldItemRepository seatHoldItemRepository;

    @GetMapping("/order-review")
    public String orderReviewPage(@RequestParam Long scheduleId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {

        String email = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        SeatHold seatHold = seatHoldRepository.findFirstByMemberIdAndScheduleIdAndStatusOrderByStartedAtDesc(
                member.getId(),
                scheduleId,
                SeatHold.Status.ACTIVE
        ).orElseThrow(() -> new IllegalStateException("선점된 좌석이 없거나 시간이 만료되었습니다. 좌석을 다시 선택해주세요."));

        // 1. 이 장바구니에 포함된 개별 좌석(SeatHoldItem) 목록 조회
        List<SeatHoldItem> holdItems = seatHoldItemRepository.findBySeatHoldIdOrderByIdAsc(seatHold.getId());

        // 기본 데이터 바인딩
        model.addAttribute("member", member);
        model.addAttribute("seatHold", seatHold);
        model.addAttribute("performanceTitle", seatHold.getSchedule().getPerformance().getTitle());
        model.addAttribute("scheduleTime", seatHold.getSchedule().getStartsAt());

        // 2. 모델에 좌석 아이템 목록과 총 가격, 총 매수 전달
        model.addAttribute("holdItems", holdItems);
        model.addAttribute("totalCount", holdItems.size());

        BigDecimal totalPrice = holdItems.stream()
                .map(item -> item.getScheduleSeat().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalPrice", totalPrice);

        // 우측 요약 패널(booking-summary)에 필요한 공연 제목과 일시 전달
        model.addAttribute("performanceTitle", seatHold.getSchedule().getPerformance().getTitle());
        model.addAttribute("scheduleTime", seatHold.getSchedule().getStartsAt());

        return "booking/order-review";
    }
}