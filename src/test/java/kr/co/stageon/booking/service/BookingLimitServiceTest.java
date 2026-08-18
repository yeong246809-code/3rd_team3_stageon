package kr.co.stageon.booking.service;

import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.booking.domain.SeatHold;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.SeatHoldItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingLimitServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SeatHoldItemRepository seatHoldItemRepository;

    @InjectMocks
    private BookingLimitService bookingLimitService;

    @Test
    void returnsZeroWhenPerformanceTicketLimitIsAlreadyReached() {
        when(reservationRepository.sumTicketCountByMemberIdAndPerformanceId(
                10L,
                104L,
                Reservation.Status.RESERVED
        )).thenReturn(4);
        when(seatHoldItemRepository.countActiveSeatsByMemberAndPerformance(
                10L,
                104L,
                SeatHold.Status.ACTIVE
        )).thenReturn(0L);

        assertThat(bookingLimitService.remainingTickets(10L, 104L, 4))
                .isZero();
    }

    @Test
    void subtractsReservationsAndActiveHeldSeatsAcrossThePerformance() {
        when(reservationRepository.sumTicketCountByMemberIdAndPerformanceId(
                10L,
                104L,
                Reservation.Status.RESERVED
        )).thenReturn(2);
        when(seatHoldItemRepository.countActiveSeatsByMemberAndPerformance(
                10L,
                104L,
                SeatHold.Status.ACTIVE
        )).thenReturn(1L);

        assertThat(bookingLimitService.remainingTickets(10L, 104L, 4))
                .isEqualTo(1);
    }
}
