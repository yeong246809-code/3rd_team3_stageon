package kr.co.stageon.booking.scheduler;

import jakarta.persistence.EntityManager;
import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.domain.SeatHold;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SeatHoldExpirationSchedulerTest {

    @Autowired
    private SeatHoldExpirationScheduler scheduler;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("만료 시간이 지난 장바구니는 EXPIRED로, 소속된 좌석은 AVAILABLE로 롤백되어야 한다.")
    void releaseExpiredSeats_Success() {
        // =========================================================
        // [1] GIVEN: JPA를 통해 더미 데이터 끌어오기 & 과거 장바구니 세팅
        // =========================================================

        // 1. 어제 생성한 8801번 실제 좌석 엔티티를 JPA로 조회
        ScheduleSeat seat = em.find(ScheduleSeat.class, 801L);

        // 2. 엔티티에 있는 비즈니스 메서드를 사용해 상태를 HELD로 안전하게 변경 (@Version 동작 보장)
        seat.release();
        seat.hold();
        em.flush(); // 좌석 상태 변경을 즉시 DB에 동기화

        // 3. 엔티티 설계가 완벽해서 '과거 시간'을 세팅할 방법이 없으므로, JPA Native Query로 강제 삽입
        em.createNativeQuery(
                        "INSERT INTO seat_holds (id, schedule_id, member_id, hold_token_hash, status, started_at, expires_at, created_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
                .setParameter(1, 9999L)
                .setParameter(2, 88L)
                .setParameter(3, 1L)
                .setParameter(4, "test-token")
                .setParameter(5, "ACTIVE")
                .setParameter(6, LocalDateTime.now().minusMinutes(15)) // 시작 시간
                .setParameter(7, LocalDateTime.now().minusMinutes(10)) // 만료 시간
                .setParameter(8, LocalDateTime.now().minusMinutes(15))
                .executeUpdate();

        em.createNativeQuery(
                        "INSERT INTO seat_hold_items (id, seat_hold_id, schedule_seat_id, created_at) " +
                                "VALUES (?, ?, ?, ?)")
                .setParameter(1, 9999L)
                .setParameter(2, 9999L)
                .setParameter(3, 801L)
                .setParameter(4, LocalDateTime.now().minusMinutes(15))
                .executeUpdate();

        // 🚨 매우 중요: 억지로 넣은 데이터와 캐시가 꼬이지 않도록 영속성 컨텍스트 초기화!
        em.flush();
        em.clear();

        // =========================================================
        // [2] WHEN: 스케줄러 실행
        // =========================================================
        scheduler.releaseExpiredSeats(); // 이 순간 JPA가 변경된 엔티티(EXPIRED, AVAILABLE)를 캐시에 저장함

        em.flush();
        em.clear();

        // =========================================================
        // [3] THEN: JPA로 엔티티를 다시 조회하여 최종 채점
        // =========================================================

        SeatHold updatedHold = em.find(SeatHold.class, 9999L);
        assertThat(updatedHold.getStatus()).isEqualTo(SeatHold.Status.EXPIRED);

        ScheduleSeat updatedSeat = em.find(ScheduleSeat.class, 801L);
        assertThat(updatedSeat.getStatus()).isEqualTo(ScheduleSeat.Status.AVAILABLE);
    }
}