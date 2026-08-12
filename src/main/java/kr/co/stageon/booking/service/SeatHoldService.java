package kr.co.stageon.booking.service;

import io.lettuce.core.AbstractRedisAsyncCommands;
import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.domain.SeatHoldItem;
import kr.co.stageon.booking.dto.SeatHoldRequest;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.SeatHoldItemRepository;
import kr.co.stageon.booking.repository.SeatHoldRepository;
import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.member.domain.Member;
import kr.co.stageon.member.repository.MemberRepository;
import kr.co.stageon.performance.domain.PerformanceSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final ScheduleSeatRepository scheduleSeatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final MemberRepository memberRepository;
    private final SeatHoldItemRepository seatHoldItemRepository;
    private final SeatRealtimeService seatRealtimeService;
    private final ReservationRepository reservationRepository;

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

        // (1) 현재 선점 중인(결제 대기 중인) 좌석 수
        int currentHoldCount = seatHoldRepository.countByMemberIdAndScheduleIdAndStatusIn(
                memberId, scheduleId, List.of(SeatHold.Status.ACTIVE, SeatHold.Status.BOOKED)
        );

        // (2) 이미 결제 완료되어 보유 중인 예약 티켓 수 (없으면 0 반환됨)
        int reservedTicketCount = reservationRepository.sumTicketCountByMemberIdAndScheduleId(
                memberId,
                scheduleId,
                Reservation.Status.RESERVED
        );

        // (3) 총합 검증: 선점 중 + 이미 예매함 + 이번에 시도하는 좌석 수
        if (currentHoldCount + reservedTicketCount + seats.size() > maxTickets) {
            throw new IllegalStateException("해당 공연의 1인 최대 예매 가능 매수(" + maxTickets + "매)를 초과합니다.");
        }

        // 3. 상태 검증 및 HELD로 상태 변경
        for (ScheduleSeat seat : seats) {
            seat.hold();
        }

        // 4. 선점 장바구니(SeatHold) 및 개별 좌석(SeatHoldItem) DB 저장

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        PerformanceSchedule schedule = seats.get(0).getSchedule();

        String holdToken = java.util.UUID.randomUUID().toString(); // 고유 해시/토큰 생성
        SeatHold seatHold = SeatHold.create(member, schedule, LocalDateTime.now().plusMinutes(5), holdToken);
        seatHoldRepository.save(seatHold);

        for (ScheduleSeat seat : seats) {
            SeatHoldItem item = SeatHoldItem.create(seatHold, seat);
            seatHoldItemRepository.save(item);
            seatRealtimeService.notifySeatStatus(seat.getId(), "RESERVED"); // 또는 "HELD"
        }
    }

    @Transactional
    public void releaseExpiredSeatHolds() {
        LocalDateTime now = LocalDateTime.now();
        // 1. 만료 시간이 지난 활성 상태의 선점 내역들을 모두 찾음
        List<SeatHold> expiredHolds = seatHoldRepository.findByStatusAndExpiresAtBefore(SeatHold.Status.ACTIVE, now);

        for (SeatHold hold : expiredHolds) {
            // 2. 선점 내역의 상태를 EXPIRED(또는 CANCELLED)로 변경
            hold.expire(); // (엔티티에 상태 변경 메서드가 없다면 hold.setStatus(Status.EXPIRED) 처리)

            // 3. 해당 선점 내역에 묶인 좌석들을 다시 AVAILABLE로 복구
            List<SeatHoldItem> items = seatHoldItemRepository.findBySeatHoldId(hold.getId());
            for (SeatHoldItem item : items) {
                item.getScheduleSeat().release(); // 좌석 상태를 AVAILABLE로 변경
            }
        }
    }
}