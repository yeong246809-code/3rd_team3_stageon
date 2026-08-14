package kr.co.stageon.performance.scheduler;

import kr.co.stageon.performance.repository.PerformanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleStatusScheduler {

    private final PerformanceScheduleRepository scheduleRepository;

    /**
     * 매 1분마다 정각(0초)에 실행되어 회차 상태를 업데이트합니다.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void updateScheduleStatuses() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 예매 오픈 처리
        int openedCount = scheduleRepository.openSchedules(now);
        if (openedCount > 0) {
            log.info("📢 [회차 상태 업데이트] {}개의 회차가 예매 OPEN 되었습니다.", openedCount);
        }

        // 2. 예매 마감 처리
        int closedCount = scheduleRepository.closeSchedules(now);
        if (closedCount > 0) {
            log.info("🔒 [회차 상태 업데이트] {}개의 회차가 예매 CLOSED 되었습니다.", closedCount);
        }
    }
}