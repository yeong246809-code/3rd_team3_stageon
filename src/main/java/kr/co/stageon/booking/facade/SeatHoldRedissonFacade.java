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
        // 1. 데드락 방지를 위해 좌석 ID를 오름차순으로 정렬
        List<Long> sortedSeatIds = request.scheduleSeatIds().stream()
                .sorted()
                .toList();

        // 2. 정렬된 ID를 기반으로 각각의 락 객체 생성
        List<RLock> locks = sortedSeatIds.stream()
                .map(id -> redissonClient.getLock("lock:seat:" + id))
                .toList();

        // 3. 여러 개의 락을 하나의 MultiLock으로 묶음
        RLock multiLock = redissonClient.getMultiLock(locks.toArray(new RLock[0]));

        try {
            // 최대 3초 동안 락 획득 대기, 획득 시 5초간 점유 후 자동 해제
            boolean isLocked = multiLock.tryLock(3, 5, TimeUnit.SECONDS);

            if (!isLocked) {
                throw new IllegalStateException("현재 선택하신 좌석을 결제 중인 다른 사용자가 있습니다. 잠시 후 다시 시도해 주세요.");
            }

            // 4. 락을 완벽히 잡은 상태에서 실제 DB 트랜잭션 서비스 호출
            seatHoldService.processSeatHolds(request);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("좌석 선점 처리 중 시스템 오류가 발생했습니다.");
        } finally {
            // 5. 작업이 끝나면 한 번에 모든 락 해제
            multiLock.unlock();
        }
    }
}