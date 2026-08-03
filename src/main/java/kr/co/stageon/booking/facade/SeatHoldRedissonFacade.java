package kr.co.stageon.booking.facade;

import kr.co.stageon.booking.dto.SeatHoldRequest;
import kr.co.stageon.booking.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldRedissonFacade {

    private final RedissonClient redissonClient;
    private final SeatHoldService seatHoldService;

    public void holdSeats(SeatHoldRequest request) {
    List<Long> sortedSeatIds = request.scheduleSeatIds()
            .stream()
            .sorted()
            .toList();

    List<RLock> locks = sortedSeatIds.stream()
            .map(id -> redissonClient.getLock("lock:seat:" + id))
            .toList();

    RLock multiLock = redissonClient.getMultiLock(
            locks.toArray(new RLock[0])
    );

    boolean locked = false;

    try {
        locked = multiLock.tryLock(3, TimeUnit.SECONDS);

        if (!locked) {
            throw new IllegalStateException(
                    "선택한 좌석을 다른 사용자가 처리 중입니다. 잠시 후 다시 시도해 주세요."
            );
        }

        seatHoldService.processSeatHolds(request);

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(
                "좌석 선점 처리 중 요청이 중단되었습니다.",
                e
        );

    } finally {
        if (locked && multiLock.isHeldByCurrentThread()) {
            multiLock.unlock();
        }
    }
}
}
