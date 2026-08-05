package kr.co.stageon.booking.api;

import kr.co.stageon.booking.domain.ScheduleSeat;
import kr.co.stageon.booking.dto.ReservationResponse;
import kr.co.stageon.booking.dto.SeatResponse;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.booking.service.BookingQueryService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** 좌석 현황과 예매내역을 제공하는 읽기 전용 REST API입니다. */
@RestController
@RequiredArgsConstructor
public class BookingQueryApiController {

    private final BookingQueryService bookingQueryService;

    @GetMapping("/api/schedules/{scheduleId}/seats")
    public List<SeatResponse> seats(@PathVariable Long scheduleId) {
        return bookingQueryService.findSeats(scheduleId);
    }

    @GetMapping("/api/reservations/{reservationId}")
    public ReservationResponse reservation(@PathVariable Long reservationId) {
        return bookingQueryService.findReservation(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예매를 찾을 수 없습니다."));
    }

    @GetMapping("/api/members/{memberId}/reservations")
    public List<ReservationResponse> memberReservations(@PathVariable Long memberId) {
        return bookingQueryService.findMemberReservations(memberId);
    }
}
