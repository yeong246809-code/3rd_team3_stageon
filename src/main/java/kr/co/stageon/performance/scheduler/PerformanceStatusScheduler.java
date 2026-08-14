package kr.co.stageon.performance.scheduler;

import kr.co.stageon.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PerformanceStatusScheduler {

    private final PerformanceRepository performanceRepository;

    /**
     * 매일 자정(00시 00분 00초)에 자동 실행됩니다.
     * 크론 표현식: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void updateEndedPerformances() {
        log.info("⏰ [공연 상태 자동 업데이트] 스케줄러 작동 시작...");

        LocalDate today = LocalDate.now();

        // 벌크 업데이트 실행 후 변경된 데이터 건수를 반환받습니다.
        int updatedCount = performanceRepository.bulkUpdateStatusToEnded(today);

        log.info("⏰ [공연 상태 자동 업데이트] 총 {}건의 공연이 ENDED(판매종료)로 변경되었습니다.", updatedCount);
    }
}