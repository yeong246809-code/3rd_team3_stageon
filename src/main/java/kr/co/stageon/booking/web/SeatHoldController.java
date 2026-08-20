package kr.co.stageon.booking.web;

import kr.co.stageon.booking.dto.SeatHoldRequest;
import kr.co.stageon.booking.facade.SeatHoldRedissonFacade;
import kr.co.stageon.booking.service.SeatHoldService;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import kr.co.stageon.queue.config.WaitingQueueProperties;
import kr.co.stageon.queue.service.RedisWaitingQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Log4j2
@Controller
@RequestMapping("/booking/seats")
@RequiredArgsConstructor
public class SeatHoldController {

    private final SeatHoldRedissonFacade seatHoldRedissonFacade;
    private final MemberRepository memberRepository;
    private final RedisWaitingQueueService waitingQueueService;
    private final SeatHoldService seatHoldService;

    @PostMapping("/hold")
    public String holdSeats(SeatHoldRequest request,
                            @CookieValue(name = WaitingQueueProperties.COOKIE_NAME, required = false) String queueToken,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            // 1. 현재 로그인한 진짜 회원 정보 조회
            Member member = memberRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("로그인 정보가 유효하지 않습니다."));

            if (!waitingQueueService.hasValidAdmission(request.scheduleId(), member.getId(), queueToken)) {
                redirectAttributes.addFlashAttribute("errorMessage", "대기열 입장 권한이 없거나 만료되었습니다.");
                return "redirect:/booking/queue?scheduleId=" + request.scheduleId();
            }

            // 2. DTO 생성자 순서에 맞게 정확히 매핑 (수정 완료)
            SeatHoldRequest securedRequest = new SeatHoldRequest(
                    member.getId(),           // 1. memberId
                    request.scheduleId(),     // 2. scheduleId
                    request.scheduleSeatIds() // 3. scheduleSeatIds
            );

            // 3. 덮어씌운 진짜 유저 정보로 분산 락 및 선점 진행
            seatHoldRedissonFacade.holdSeats(securedRequest);

            // 4. 성공 시 주문서 페이지로 이동
            return "redirect:/booking/order-review?scheduleId=" + securedRequest.scheduleId();

        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/booking/seats?scheduleId=" + request.scheduleId();
        } catch (Exception e) {
            log.error("좌석 선점 처리 중 예상치 못한 서버 에러 발생: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "서버 처리 중 오류가 발생했습니다.");

            Long fallbackId = request.scheduleId() != null ? request.scheduleId() : 1L;
            return "redirect:/booking/seats?scheduleId=" + fallbackId;
        }
    }

    @ResponseBody
    @PostMapping("/holds/{seatHoldId}/cancel")
    public ResponseEntity<Void> cancelSeatHold(@PathVariable Long seatHoldId) {
        seatHoldService.cancelHoldImmediately(seatHoldId);
        return ResponseEntity.ok().build();
    }
}
