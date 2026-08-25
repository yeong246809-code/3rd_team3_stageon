package kr.co.stageon.booking.service;

import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.booking.domain.ReservationSeat;
import kr.co.stageon.booking.dto.MyTicketResponse;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.ReservationSeatRepository;
import kr.co.stageon.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyTicketQueryService {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final TicketRepository ticketRepository;
    private final QrCodeService qrCodeService;

    /**
     * 로그인 회원의 보유 티켓 조회
     * 좌석 1개 = 모바일 티켓 1장
     */
    public List<MyTicketResponse> findMemberTickets(Long memberId) {

        List<Reservation> reservations =
                reservationRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

        List<MyTicketResponse> tickets = new ArrayList<>();

        for (Reservation reservation : reservations) {

            // 확정된 예매만 보유 티켓에 노출
            if (reservation.getStatus() != Reservation.Status.RESERVED) {
                continue;
            }

            List<ReservationSeat> reservationSeats =
                    reservationSeatRepository
                            .findByReservationIdOrderByIdAsc(reservation.getId());

            var schedule = reservation.getSchedule();
            var performance = schedule.getPerformance();
            var hall = schedule.getVenueHall();
            var venue = hall.getVenue();

            for (ReservationSeat seat : reservationSeats) {

                kr.co.stageon.ticket.domain.Ticket ticket = ticketRepository.findByReservationSeatId(seat.getId()).orElse(null);

                String ticketStatus;
                String qrData = null;

                if (ticket != null && ticket.getStatus() == kr.co.stageon.ticket.domain.Ticket.Status.USED) {
                    ticketStatus = "ENTERED";
                } else {
                    ticketStatus = determineTicketStatus(schedule.getStartsAt(), performance.getRuntimeMinutes());
                }

                if ("AVAILABLE".equals(ticketStatus)) {
                    if (ticket != null) {
                        qrData = ticket.getQrTokenHash();
                    } else {
                        qrData = "STAGEON:TICKET:" + seat.getId();
                    }
                }

                MyTicketResponse ticketRes =
                        new MyTicketResponse(
                                seat.getId(),
                                reservation.getId(),
                                reservation.getBookingNumber(),

                                performance.getTitle(),
                                performance.getPosterUrl(),
                                schedule.getStartsAt(),

                                venue.getName(),
                                hall.getName(),

                                seat.getCapturedGradeName(),
                                seat.getCapturedSectionName(),
                                seat.getCapturedRowLabel(),
                                seat.getCapturedSeatNumber(),
                                seat.getCapturedUnitPrice(),

                                ticketStatus,
                                reservation.getReceiveMethod().name(),
                                qrData
                        );

                tickets.add(ticketRes);
            }
        }

        LocalDateTime now = LocalDateTime.now();

        return tickets.stream()
                .filter(t -> {
                    if (t.startsAt() == null) return true;
                    return t.startsAt().plusDays(1).isAfter(now);
                })
                .sorted((t1, t2) -> {
                    boolean t1Ended = "ENDED".equals(t1.ticketStatus());
                    boolean t2Ended = "ENDED".equals(t2.ticketStatus());

                    if (t1Ended && !t2Ended) return 1;
                    if (!t1Ended && t2Ended) return -1;

                    if (t1.startsAt() == null && t2.startsAt() == null) return 0;
                    if (t1.startsAt() == null) return 1;
                    if (t2.startsAt() == null) return -1;

                    return t1.startsAt().compareTo(t2.startsAt());
                })
                .toList();
    }

    /**
     * 🚨 [추가됨] 스캐너에 띄워줄 단건 예매 정보 조회 (MyTicketResponse 재활용)
     */
    public MyTicketResponse getTicketBySeatId(Long seatId) {
        ReservationSeat seat = reservationSeatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        var reservation = seat.getReservation();
        var schedule = reservation.getSchedule();
        var performance = schedule.getPerformance();
        var hall = schedule.getVenueHall();
        var venue = hall.getVenue();

        kr.co.stageon.ticket.domain.Ticket ticket = ticketRepository.findByReservationSeatId(seatId).orElse(null);
        String ticketStatus = (ticket != null && ticket.getStatus() == kr.co.stageon.ticket.domain.Ticket.Status.USED) ? "ENTERED" : "AVAILABLE";

        return new MyTicketResponse(
                seat.getId(),
                reservation.getId(),
                reservation.getBookingNumber(),
                performance.getTitle(),
                performance.getPosterUrl(),
                schedule.getStartsAt(),
                venue.getName(),
                hall.getName(),
                seat.getCapturedGradeName(),
                seat.getCapturedSectionName(),
                seat.getCapturedRowLabel(),
                seat.getCapturedSeatNumber(),
                seat.getCapturedUnitPrice(),
                ticketStatus,
                reservation.getReceiveMethod().name(),
                ticket != null ? ticket.getQrTokenHash() : null
        );
    }

    /**
     * 🚨 [추가됨] 직원이 스캐너에서 입장 처리할 때 호출되는 쓰기 메서드
     * 클래스 레벨의 readOnly = true를 덮어쓰기 위해 @Transactional을 붙임
     */
    @Transactional
    public void processTicketEntry(Long seatId) {
        kr.co.stageon.ticket.domain.Ticket ticket = ticketRepository.findByReservationSeatId(seatId)
                .orElseThrow(() -> new IllegalArgumentException("발급된 티켓이 없습니다."));

        if (ticket.getStatus() == kr.co.stageon.ticket.domain.Ticket.Status.USED) {
            throw new IllegalStateException("이미 입장 처리된 티켓입니다.");
        }

        // Ticket 엔티티의 상태 변경 메서드 호출
        ticket.use();
    }

    /**
     * 티켓 화면 상태 계산
     */
    private String determineTicketStatus(
            LocalDateTime startsAt,
            Integer runtimeMinutes
    ) {
        if (startsAt == null) {
            return "UPCOMING";
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime qrOpenAt = startsAt.minusHours(2);

        if (now.isBefore(qrOpenAt)) {
            return "UPCOMING";
        }

        int safeRuntime = (runtimeMinutes != null && runtimeMinutes > 0) ? runtimeMinutes : 120;
        LocalDateTime endTime = startsAt.plusMinutes(safeRuntime);

        if (!now.isBefore(endTime)) {
            return "ENDED";
        }

        return "AVAILABLE";
    }
}