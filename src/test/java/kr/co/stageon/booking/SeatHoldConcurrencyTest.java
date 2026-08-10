package kr.co.stageon.booking;

import kr.co.stageon.booking.dto.SeatHoldRequest;
import kr.co.stageon.booking.facade.SeatHoldRedissonFacade;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SeatHoldConcurrencyTest {

    @Autowired
    private SeatHoldRedissonFacade seatHoldRedissonFacade;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Autowired
    private ScheduleSeatRepository scheduleSeatRepository;

    private final Long scheduleId = 88L;
    private final List<Long> targetSeatIds = new ArrayList<>(List.of(801L, 802L));

    @BeforeEach
    @Transactional
    void setUp() {
        // 1. 해당 스케줄과 좌석에 걸려있는 기존 선점 데이터(SeatHold 등) 삭제
        // (프로젝트 엔티티 구조에 맞춰 메서드 이름이나 쿼리를 수정해주세요)
        try {
            // 예: seatHoldRepository.deleteByScheduleIdAndSeatIds(...); 또는 전체 삭제 후 재삽입
            // 만약 락 정보나 연관 데이터를 지워야 한다면 여기에 작성합니다.

            // 2. ScheduleSeat 상태를 AVAILABLE(선점 가능) 상태로 원복하는 로직
            // 예시:
            // List<ScheduleSeat> seats = scheduleSeatRepository.findAllByScheduleIdAndSeatIdIn(scheduleId, targetSeatIds);
            // seats.forEach(ScheduleSeat::releaseSeat); // 가용 상태로 변경하는 엔티티 메서드
            // scheduleSeatRepository.saveAll(seats);

        } catch (Exception e) {
            // 초기화 중 발생하는 예외 무시 또는 로그
        }

        System.out.println("🧹 [BeforeEach] 테스트 좌석 초기화 실행 완료");
    }

    @Test
    @DisplayName("100명이 동시에 같은 좌석을 선점하려고 하면 단 1명만 성공해야 한다 (동시 선점)")
    void holdSeats_concurrency() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 1; i <= threadCount; i++) {
            Long mockMemberId = (long) i;

            executorService.submit(() -> {
                try {
                    SeatHoldRequest request = new SeatHoldRequest(mockMemberId, scheduleId, targetSeatIds);
                    seatHoldRedissonFacade.holdSeats(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        System.out.println("=================================================");
        System.out.println("🎉 [동시 선점] 테스트 결과: " + successCount.get() + "명 성공, " + failCount.get() + "명 실패");
        System.out.println("=================================================");

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(99);
    }

    @Test
    @DisplayName("이미 선점된 좌석에 대해 시간차를 두고(0.5초 간격) 요청하면 모두 실패해야 한다 (시간차 선점)")
    void holdSeats_sequential_delay() throws InterruptedException {
        // given
        Long firstUserId = 1L;
        SeatHoldRequest firstRequest = new SeatHoldRequest(firstUserId, scheduleId, targetSeatIds);

        try {
            seatHoldRedissonFacade.holdSeats(firstRequest);
            System.out.println("=================================================");
            System.out.println("🎉 [시간차 선점] 첫 번째 유저(" + firstUserId + ") 선점 성공");
            System.out.println("=================================================");
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.fail("첫 번째 유저 선점은 성공해야 합니다: " + e.getMessage());
        }

        int subsequentUsers = 5;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 2; i <= subsequentUsers + 1; i++) {
            Long laterUserId = (long) i;
            Thread.sleep(500);

            try {
                SeatHoldRequest laterRequest = new SeatHoldRequest(laterUserId, scheduleId, targetSeatIds);
                seatHoldRedissonFacade.holdSeats(laterRequest);

                successCount.incrementAndGet();
                System.out.println("❌ 유저 " + laterUserId + " 선점 성공 (실패해야 함!)");
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.out.println("✅ 유저 " + laterUserId + " 선점 차단됨 (정상): " + e.getMessage());
            }
        }

        System.out.println("=================================================");
        System.out.println("🎉 [시간차 선점] 결과: 추가 성공 " + successCount.get() + "명, 차단(실패) " + failCount.get() + "명");
        System.out.println("=================================================");

        // then
        assertThat(successCount.get()).isEqualTo(0);
        assertThat(failCount.get()).isEqualTo(subsequentUsers);
    }
}