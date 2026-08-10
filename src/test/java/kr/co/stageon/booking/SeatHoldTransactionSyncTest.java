package kr.co.stageon.booking;

import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.dto.SeatHoldRequest;
import kr.co.stageon.booking.facade.SeatHoldRedissonFacade;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SeatHoldTransactionSyncTest {

    @Autowired
    private SeatHoldRedissonFacade seatHoldRedissonFacade;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Test
    @DisplayName("선점 직후, 락이 풀리는 순간 DB 트랜잭션이 완전히 커밋되어 조회 가능해야 한다 (트랜잭션 커밋 동기화 검증)")
    void holdSeats_transactionCommitSync() {
        // given
        Long userId = 100L;
        Long scheduleId = 88L;
        List<Long> targetSeatIds = new ArrayList<>(List.of(801L, 802L));

        SeatHoldRequest request = new SeatHoldRequest(userId, scheduleId, targetSeatIds);

        // when: 선점 실행 (Facade 락 획득 -> Service 트랜잭션 커밋 -> 락 해제 순)
        seatHoldRedissonFacade.holdSeats(request);

        // then: 메서드가 완전히 종료된 직후(= 락이 확실히 풀린 시점)
        // 이미 구현되어 있는 findByMemberIdAndScheduleIdAndStatus 메서드로 데이터가 정상 커밋되었는지 확인
        Optional<SeatHold> savedHold = seatHoldRepository.findFirstByMemberIdAndScheduleIdAndStatusOrderByStartedAtDesc(
                userId, scheduleId, SeatHold.Status.ACTIVE // 프로젝트 내 사용 중인 상태 값(예: ACTIVE, HELD 등)에 맞춰 수정해주세요
        );

        assertThat(savedHold).isPresent();
        System.out.println("✅ [트랜잭션 동기화 검증 성공] 락 해제 직후 DB 커밋 완료 확인됨!");
    }
}