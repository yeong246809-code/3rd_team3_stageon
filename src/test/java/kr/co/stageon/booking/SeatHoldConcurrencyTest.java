package kr.co.stageon.booking;

import kr.co.stageon.booking.dto.SeatHoldRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import kr.co.stageon.booking.facade.SeatHoldRedissonFacade;

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

    @Test
    @DisplayName("100명이 동시에 같은 좌석을 선점하려고 하면 단 1명만 성공해야 한다.")
    void holdSeats_concurrency() throws InterruptedException {
        // given
        int threadCount = 100; // 동시에 100명 접속 시뮬레이션
        // 스레드 풀 100개 생성 (100명의 유저 역할)
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        // 모든 스레드가 작업을 마칠 때까지 기다리기 위한 장치
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 성공 및 실패 횟수를 안전하게 카운트할 객체
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 테스트용 좌석 ID (DB에 있는 실제 좌석 ID로 변경하세요)
        List<Long> targetSeatIds = new ArrayList<>(List.of(801L, 802L));
        Long scheduleId = 88L;

        // when
        for (int i = 1; i <= threadCount; i++) {
            Long mockMemberId = (long) i; // 1번~100번 유저 생성

            executorService.submit(() -> {
                try {
                    // 각 유저가 동일한 회차의 동일한 좌석 선점 요청
                    SeatHoldRequest request = new SeatHoldRequest(mockMemberId, scheduleId, targetSeatIds);
                    seatHoldRedissonFacade.holdSeats(request);

                    // 예외가 안 터지고 여기까지 왔다면 선점 성공
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 락을 획득하지 못했거나 이미 HELD 상태라서 예외가 터지면 실패로 카운트
                    failCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown(); // 스레드 작업 완료 알림
                }
            });
        }

        // 100명의 유저가 모두 요청을 끝낼 때까지 메인 스레드 대기
        latch.await();
        executorService.shutdown();

        System.out.println("=================================================");
        System.out.println("🎉 테스트 결과: " + successCount.get() + "명 성공, " + failCount.get() + "명 실패");
        System.out.println("=================================================");

        // then
        // 100명 중 단 1명만 성공하고, 99명은 튕겨나가야(실패해야) 정상!
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(99);
    }
}