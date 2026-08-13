package kr.co.stageon.booking.web;

import kr.co.stageon.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationApiController {

    private final ReservationService reservationService;

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelReservationSeats(@RequestBody CancelRequest request) {

        reservationService.cancelSeats(
                request.reservationId(),
                request.reservationSeatIds(),
                request.cancelReason(),
                request.refundBank(),
                request.refundAccountNumber(),
                request.refundHolderName()
        );

        return ResponseEntity.ok("취소가 완료되었습니다.");
    }

    public record CancelRequest(
            Long reservationId,
            List<Long> reservationSeatIds,
            String cancelReason,
            String refundBank,
            String refundAccountNumber,
            String refundHolderName
    ) {}
}