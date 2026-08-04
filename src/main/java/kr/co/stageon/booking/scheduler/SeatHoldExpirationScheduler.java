package kr.co.stageon.booking.scheduler;

import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.domain.SeatHoldItem;
import kr.co.stageon.booking.repository.SeatHoldItemRepository;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldExpirationScheduler {

    private final SeatHoldRepository seatHoldRepository;
    private final SeatHoldItemRepository seatHoldItemRepository;

    // 1분마다 실행
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void releaseExpiredSeats() {
        log.info("⏰ [좌석 선점 만료 스케줄러] 작동 시작...");

        LocalDateTime now = LocalDateTime.now();

        // 1. 만료 시간이 지난 'ACTIVE' 상태의 장바구니(SeatHold) 조회
        List<SeatHold> expiredHolds = seatHoldRepository.findByStatusAndExpiresAtBefore(
                SeatHold.Status.ACTIVE, now
        );

        if (expiredHolds.isEmpty()) {
            return; // 만료된 게 없으면 조용히 종료
        }

        // 2. 만료된 장바구니들의 상태를 'EXPIRED'로 변경
        for (SeatHold hold : expiredHolds) {
            hold.expire();
        }

        // 3. 만료된 장바구니 안에 들어있던 개별 좌석들(SeatHoldItem) 싹 다 조회
        List<SeatHoldItem> expiredItems = seatHoldItemRepository.findBySeatHoldIn(expiredHolds);

        // 4. 개별 좌석들을 다시 예매 가능 상태로 롤백
        for (SeatHoldItem item : expiredItems) {
            item.getScheduleSeat().release();
            log.info("회수된 좌석 ID: {}", item.getScheduleSeat().getId());
        }

        log.info("⏰ 총 {}개의 장바구니와 {}개의 좌석이 만료되어 회수되었습니다.", expiredHolds.size(), expiredItems.size());
    }
}