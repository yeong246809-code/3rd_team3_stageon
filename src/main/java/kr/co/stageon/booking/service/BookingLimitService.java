package kr.co.stageon.booking.service;

import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.SeatHoldItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원별 공연 예매 한도를 한 곳에서 계산합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingLimitService {

    private final ReservationRepository reservationRepository;
    private final SeatHoldItemRepository seatHoldItemRepository;

    public int usedTickets(Long memberId, Long performanceId) {
        int reservedTickets = reservationRepository
                .sumTicketCountByMemberIdAndPerformanceId(
                        memberId,
                        performanceId,
                        Reservation.Status.RESERVED
                );
        long heldTickets = seatHoldItemRepository
                .countActiveSeatsByMemberAndPerformance(
                        memberId,
                        performanceId,
                        SeatHold.Status.ACTIVE
                );

        return Math.toIntExact((long) reservedTickets + heldTickets);
    }

    public int remainingTickets(Long memberId, Long performanceId, int maxTickets) {
        return Math.max(0, maxTickets - usedTickets(memberId, performanceId));
    }
}
