package kr.co.stageon.booking.web;

import kr.co.stageon.booking.dto.ReservationDetailResponse;
import kr.co.stageon.booking.repository.ScheduleSeatRepository;
import kr.co.stageon.booking.service.BookingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    /*@GetMapping({"/booking/complete", "/booking-complete"})
    public String bookingComplete(@RequestParam Long reservationId, Model model) {

        // 1. 예약 ID로 상세 정보 조회 (기존에 만들어두신 DTO 활용)
        ReservationDetailResponse reservation = bookingQueryService.getReservationDetail(reservationId);

        // 2. HTML(Thymeleaf)에서 쓸 수 있게 Model에 담기
        model.addAttribute("reservation", reservation);

        // 3. templates/booking/booking-complete.html 반환
        return "booking/booking-complete";
    }*/

    @GetMapping("/booking/complete")
    public String bookingComplete(@RequestParam("reservationId") Long reservationId, Model model) {

        // 1. 서비스에 세팅해둔 1원짜리 가짜(Mock) 데이터를 가져옵니다.
        ReservationDetailResponse reservation = bookingQueryService.getReservationDetail(reservationId);

        // 2. HTML 화면에서 쓸 수 있도록 모델에 담아줍니다.
        model.addAttribute("reservation", reservation);

        // 3. 완료 페이지 렌더링
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
