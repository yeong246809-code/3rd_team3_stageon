package kr.co.stageon.booking.service;

import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.dto.SeatHoldRequest;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
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
    public void processSeatHolds(SeatHoldRequest request) {
        List<Long> seatIds = request.scheduleSeatIds();
        Long memberId = request.memberId();
        Long scheduleId = request.scheduleId();

        // 1. 요청받은 좌석들 DB에서 조회
        List<ScheduleSeat> seats = scheduleSeatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new IllegalArgumentException("일부 좌석 정보를 찾을 수 없습니다.");
        }

        // 2. 최대 예매 가능 매수 검증 (기존 보유 좌석 + 이번에 선택한 좌석)
        int maxTickets = seats.get(0).getSchedule().getMaxTicketsPerMember();
        int currentHoldCount = seatHoldRepository.countByMemberIdAndScheduleIdAndStatusIn(
                memberId, scheduleId, List.of(SeatHold.Status.ACTIVE, SeatHold.Status.BOOKED)
        );

        if (currentHoldCount + seats.size() > maxTickets) {
            throw new IllegalStateException("해당 공연의 1인 최대 예매 가능 매수(" + maxTickets + "매)를 초과합니다.");
        }

        // 3. 상태 검증 및 HELD로 상태 변경
        for (ScheduleSeat seat : seats) {
            // 이전에 엔티티에 만들어둔 비즈니스 메서드 호출 (이미 팔린 자리면 예외 발생)
            seat.hold();
        }

        // 4. (팀원분이 만들어둔 로직에 맞춰 SeatHold를 INSERT 하는 부분 추가)
        // SeatHold seatHold = SeatHold.create(...);
        // seatHoldRepository.save(seatHold);
    }
}