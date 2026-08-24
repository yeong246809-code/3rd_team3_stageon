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

                // 공연 시간 기준으로 현재 티켓 상태 계산
                String ticketStatus =
                        determineTicketStatus(
                                schedule.getStartsAt(),
                                performance.getRuntimeMinutes()
                        );


                /*
                 * QR은 공연 시작 2시간 전부터만 생성
                 *
                 * UPCOMING  → QR 없음
                 * AVAILABLE → QR 생성
                 * ENDED     → QR 없음
                 */
                String qrData = null;
                if ("AVAILABLE".equals(ticketStatus)) {
                    // DB에서 해당 좌석의 Ticket 엔티티를 찾아 qrTokenHash를 가져옵니다.
                    qrData = ticketRepository.findByReservationSeatId(seat.getId())
                            .map(kr.co.stageon.ticket.domain.Ticket::getQrTokenHash)
                            .orElse("STAGEON:TICKET:" + seat.getId()); // Ticket이 없을 때의 임시 Fallback
                }


                MyTicketResponse ticket =
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
                                qrData
                        );

                tickets.add(ticket);
            }
        }

        // 🚨 필터링 및 정렬 로직 추가
        LocalDateTime now = LocalDateTime.now();

        return tickets.stream()
                // 1. [필터링] 공연 시작 24시간이 지나면 화면에서 숨김 처리
                .filter(ticket -> {
                    if (ticket.startsAt() == null) return true;
                    return ticket.startsAt().plusDays(1).isAfter(now);
                })
                // 2. [정렬] ENDED는 맨 밑으로, 나머지는 날짜 오름차순(빠른 순) 정렬
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
     * 티켓 화면 상태 계산
     *
     * UPCOMING  : 공연 시작 24시간 이전
     * AVAILABLE : 공연 시작 24시간 전 ~ 공연 종료 전
     * ENDED     : 공연 종료 이후
     */
    private String determineTicketStatus(
            LocalDateTime startsAt,
            Integer runtimeMinutes
    ) {

        // 공연 시작 정보가 없으면 QR 오픈 전으로 처리
        if (startsAt == null) {
            return "UPCOMING";
        }

        LocalDateTime now = LocalDateTime.now();

        // QR 공개 시각 = 공연 시작 2시간 전
        LocalDateTime qrOpenAt =
                startsAt.minusHours(2);


        // 아직 QR 공개 전
        if (now.isBefore(qrOpenAt)) {
            return "UPCOMING";
        }


        // 러닝타임이 있다면 실제 공연 종료 시각 계산
        if (runtimeMinutes != null && runtimeMinutes > 0) {

            LocalDateTime endTime =
                    startsAt.plusMinutes(runtimeMinutes);

            // 공연 종료 시각과 같거나 지난 경우
            if (!now.isBefore(endTime)) {
                return "ENDED";
            }
        }


        // 공연 시작 2시간 전 ~ 공연 종료 전
        return "AVAILABLE";
    }
}