package kr.co.stageon.booking.web;

import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.domain.SeatHoldItem;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.booking.repository.SeatHoldItemRepository;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import kr.co.stageon.booking.service.SeatRealtimeService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    // 화면에서 좌석을 클릭하는 순간 호출되는 비동기 API
    @PostMapping("/{scheduleSeatId}/check")
    public ResponseEntity<?> checkAndTempHoldSeat(@PathVariable Long scheduleSeatId) {

        // 1. DB 1차 상태 검증 (이미 결제까지 완료된 좌석인지)
        ScheduleSeat seat = scheduleSeatRepository.findById(scheduleSeatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        if (seat.getStatus() != ScheduleSeat.Status.AVAILABLE) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 예매된 좌석입니다.");
        }

        // 2. Redis를 이용한 '클릭' 순간의 임시 선점 (화면 동시 클릭 방지)
        // 키 이름: seat:selecting:{좌석ID}
        RBucket<String> tempLockBucket = redissonClient.getBucket("seat:selecting:" + scheduleSeatId);

        // trySet: 키가 없으면 생성(true 반환), 이미 누가 클릭해서 키가 있으면 생성 안함(false 반환)
        // 화면에서 클릭한 상태를 10분간만 유지 (시간 초과 시 자동 해제)
        boolean isTempLocked = tempLockBucket.trySet("LOCKED", 10, TimeUnit.MINUTES);

        if (!isTempLocked) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("이미 선점된 좌석입니다.");
        }

        // 정상적으로 클릭 가능
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
    public ResponseEntity<?> releaseTempHoldSeat(@PathVariable Long scheduleSeatId) {

        // 1. 체크할 때 걸어뒀던 Redis 키를 똑같이 찾아옵니다.
        RBucket<String> tempLockBucket = redissonClient.getBucket("seat:selecting:" + scheduleSeatId);

        // 2. 키가 존재하면 즉시 삭제하여 락을 풉니다.
        tempLockBucket.delete();

        return ResponseEntity.ok("선택 해제 완료");
    }

}