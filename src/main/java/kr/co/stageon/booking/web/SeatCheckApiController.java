package kr.co.stageon.booking.web;

import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.domain.SeatHoldItem;
import kr.co.stageon.booking.dto.SeatResponse;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.booking.repository.SeatHoldItemRepository;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import kr.co.stageon.booking.service.BookingQueryService;
import kr.co.stageon.booking.service.SeatRealtimeService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatCheckApiController {

    private final ScheduleSeatRepository scheduleSeatRepository;
    private final RedissonClient redissonClient;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatHoldItemRepository seatHoldItemRepository;
    private final SeatRealtimeService seatRealtimeService;
    private final BookingQueryService bookingQueryService;

    // 화면에서 좌석을 클릭하는 순간 호출되는 비동기 API
    @PostMapping("/{scheduleSeatId}/check")
    public ResponseEntity<?> checkAndTempHoldSeat(
            @PathVariable Long scheduleSeatId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        // 로그인한 유저의 식별자(ID 또는 이메일)
        String currentUserId = userDetails.getUsername();

        // 1. DB 1차 상태 검증
        ScheduleSeat seat = scheduleSeatRepository.findById(scheduleSeatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        if (seat.getStatus() != ScheduleSeat.Status.AVAILABLE) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 예매된 좌석입니다.");
        }

        // 2. Redis를 이용한 '클릭' 순간의 임시 선점
        RBucket<String> tempLockBucket = redissonClient.getBucket("seat:selecting:" + scheduleSeatId);

        // 🚨 기존 "LOCKED" 대신 현재 유저의 식별자(currentUserId)를 값으로 저장합니다.
        boolean isTempLocked = tempLockBucket.trySet(currentUserId, 10, TimeUnit.MINUTES);

        if (!isTempLocked) {
            // 누군가 이미 락을 잡고 있는 경우, 그 주인이 나 자신인지 확인 (네트워크 지연 재요청 등 방어)
            String lockOwner = tempLockBucket.get();
            if (currentUserId.equals(lockOwner)) {
                return ResponseEntity.ok("선택 가능"); // 이미 본인이 잡고 있음
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 선점된 좌석입니다.");
        }

        seatRealtimeService.notifySeatStatus(scheduleSeatId, "HELD");

        return ResponseEntity.ok("선택 가능");
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSeats() {
        return seatRealtimeService.subscribe();
    }

    @PostMapping("/holds/{seatHoldId}/cancel")
    @Transactional
    public ResponseEntity<?> cancelHoldOnLeave(@PathVariable Long seatHoldId) {

        seatHoldRepository.findById(seatHoldId).ifPresent(seatHold -> {
            if (seatHold.getStatus() == SeatHold.Status.ACTIVE) {
                seatHold.cancel();

                List<SeatHoldItem> items = seatHoldItemRepository.findBySeatHoldIdOrderByIdAsc(seatHoldId);

                List<Long> seatIds = items.stream()
                        .map(item -> item.getScheduleSeat().getId())
                        .toList();

                if (!seatIds.isEmpty()) {
                    scheduleSeatRepository.bulkReleaseSeats(seatIds);
                }

                redissonClient.getBucket("seat:selecting:" + seatIds).delete();

                for (Long seatId : seatIds) {
                    redissonClient.getBucket("seat:selecting:" + seatId).delete();
                    seatRealtimeService.notifySeatStatus(seatId, "AVAILABLE");
                }
            }
        });

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{scheduleSeatId}/uncheck")
    public ResponseEntity<?> releaseTempHoldSeat(
            @PathVariable Long scheduleSeatId,
            @AuthenticationPrincipal UserDetails userDetails) { // ✅ 유저 정보 추가

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        String currentUserId = userDetails.getUsername();

        // 1. 체크할 때 걸어뒀던 Redis 키를 찾아옵니다.
        RBucket<String> tempLockBucket = redissonClient.getBucket("seat:selecting:" + scheduleSeatId);
        String lockOwner = tempLockBucket.get();

        // 🚨 2. 락의 주인이 현재 요청을 보낸 유저(본인)와 일치할 때만 해제합니다.
        if (lockOwner != null && lockOwner.equals(currentUserId)) {
            tempLockBucket.delete();
            return ResponseEntity.ok("선택 해제 완료");
        } else if (lockOwner == null) {
            // 이미 10분이 지나서 만료되었거나 풀린 경우
            return ResponseEntity.ok("이미 선택 해제가 완료된 좌석입니다.");
        } else {
            // 🚨 다른 사람이 락을 풀려고 시도하는 경우 (악의적 접근 차단)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("본인이 선점한 좌석만 해제할 수 있습니다.");
        }
    }

    @GetMapping("/schedules/{scheduleId}")
    public ResponseEntity<List<SeatResponse>> getSeatsStatus(@PathVariable Long scheduleId) {
        // 위에서 수정한 '본인 락 구분'이 적용된 findSeats를 반환합니다.
        List<SeatResponse> seats = bookingQueryService.findSeats(scheduleId);
        return ResponseEntity.ok(seats);
    }

}