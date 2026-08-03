package kr.co.stageon.booking.service;

import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import kr.co.stageon.booking.domain.ScheduleSeat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final ScheduleSeatRepository scheduleSeatRepository;
    private final SeatHoldRepository seatHoldRepository;

    @Transactional
    public void processSeatHold(Long scheduleSeatId, Long memberId, Long scheduleId) {

        // 1. 좌석 재고 확인
        ScheduleSeat seat = scheduleSeatRepository.findById(scheduleSeatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        // 2. 좌석 상태 검증 (동시성 락을 뚫고 들어와도 여기서 한 번 더 막힘)
        if (!"AVAILABLE".equals(seat.getStatus().name())) {
            throw new IllegalStateException("앗! 간발의 차이로 다른 사용자가 먼저 선점한 좌석입니다.");
        }

        // 3. 🚨 공연별 최대 예매 가능 매수 검증 로직 추가
        int maxTickets = seat.getSchedule().getMaxTicketsPerMember();
        int currentHoldCount = seatHoldRepository.countByMemberIdAndScheduleIdAndStatusIn(
                memberId,
                scheduleId,
                List.of(SeatHold.Status.ACTIVE, SeatHold.Status.BOOKED)
        );

        if (currentHoldCount >= maxTickets) {
            throw new IllegalStateException("해당 공연의 1인 최대 예매 가능 매수(" + maxTickets + "매)를 초과했습니다.");
        }

        // 4. 상태 변경 (AVAILABLE -> HELD)
        seat.hold(); // (엔티티에 팩토리나 Setter 메서드가 있다고 가정)

        // 5. (선택) SeatHold, SeatHoldItem 테이블에 선점 정보 INSERT 하는 로직 추가...
        // seatHoldRepository.save(...);
    }
}