package kr.co.stageon.booking.web;

import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.booking.service.BookingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

/** 회차 선택 이후 예매 단계 화면을 순서대로 연결합니다. */
@Controller
@RequiredArgsConstructor
public class BookingViewController {

    private final BookingQueryService bookingQueryService;
    private final ScheduleSeatRepository scheduleSeatRepository;

    /*@GetMapping({"/booking/queue", "/queue"})
    public String queue(@RequestParam(required = false) Long scheduleId, Model model) {
        model.addAttribute("scheduleId", scheduleId);
        return "booking/queue";
    }*/

    @GetMapping({"/booking/seats", "/seat-select"})
    public String seats(@RequestParam(required = false) Long scheduleId, Model model) {
        model.addAttribute("scheduleId", scheduleId);

        if (scheduleId == null) {
            model.addAttribute("groupedSeats", Collections.emptyMap());
        } else {
            model.addAttribute("groupedSeats", bookingQueryService.findGroupedSeats(scheduleId));
            model.addAttribute("seatGrades", bookingQueryService.findSeatGrades(scheduleId));

            BookingQueryService.ScheduleSummaryResponse summary = bookingQueryService.findScheduleSummary(scheduleId);

            if (summary != null) {
                model.addAttribute("performanceTitle", summary.performanceTitle());
                model.addAttribute("scheduleTime", summary.scheduleTime());
            }
        }

        return "booking/seat-select";
    }

    /*@GetMapping({"/booking/order", "/order-review"})
    public String order(@RequestParam(required = false) Long reservationId, Model model) {
        addReservation(reservationId, model);
        return "booking/order-review";
    }*/

    @GetMapping({"/booking/payment", "/mock-payment"})
    public String payment(@RequestParam(required = false) Long reservationId, Model model) {
        addReservation(reservationId, model);
        return "booking/mock-payment";
    }

    @GetMapping({"/booking/complete", "/booking-complete"})
    public String complete(@RequestParam(required = false) Long reservationId, Model model) {
        addReservation(reservationId, model);
        return "booking/booking-complete";
    }

    private void addReservation(Long reservationId, Model model) {
        model.addAttribute("reservationId", reservationId);
        if (reservationId != null) {
            bookingQueryService.findReservation(reservationId)
                    .ifPresent(reservation -> model.addAttribute("reservation", reservation));
        }
    }
}
