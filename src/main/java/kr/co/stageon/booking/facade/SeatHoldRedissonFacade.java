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
        // 1. DTO에서 좌석 ID 리스트를 꺼내 오름차순 정렬 (데드락 방지)
        // 주의: DTO의 getter 메서드명(getScheduleSeatIds)이 다르면 본인 코드에 맞게 수정하세요!
        List<Long> sortedSeatIds = request.scheduleSeatIds()
                .stream()
                .sorted()
                .toList();

        // 2. 정렬된 ID를 바탕으로 각각의 RLock 객체 생성
        List<RLock> locks = sortedSeatIds.stream()
                .map(id -> redissonClient.getLock("lock:seat:" + id))
                .toList();

        // 3. 여러 개의 락을 하나로 묶는 MultiLock 생성
        RLock multiLock = redissonClient.getMultiLock(
                locks.toArray(new RLock[0])
        );

        boolean isLocked = false; // 락 획득 여부 상태 변수

        try {
            // 4. 락 획득 시도: 최대 5초 대기, 락 획득 후 3초 뒤 자동 해제 (장애 방지)
            isLocked = multiLock.tryLock(5, 10, TimeUnit.SECONDS);

            if (!isLocked) {
                throw new IllegalStateException("선택한 좌석을 다른 사용자가 처리 중입니다. 잠시 후 다시 시도해 주세요.");
            }

            // 5. 락 획득 성공 시 비즈니스 로직 실행
            seatHoldService.processSeatHolds(request);

        } catch (InterruptedException e) {
            // 스레드가 인터럽트 되었을 때의 처리
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 대기 시간이 초과되었거나 오류가 발생했습니다.");
        } finally {
            // 6. 락을 정상적으로 획득했을 때만 안전하게 해제
            if (isLocked) {
                multiLock.unlock();
            }
        }
    }
}