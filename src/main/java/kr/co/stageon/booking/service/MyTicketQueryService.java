package kr.co.stageon.booking.service;

import kr.co.stageon.booking.domain.Reservation;
import kr.co.stageon.booking.domain.ReservationSeat;
import kr.co.stageon.booking.dto.MyTicketResponse;
import kr.co.stageon.booking.repository.ReservationRepository;
import kr.co.stageon.booking.repository.ReservationSeatRepository;
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
    private final QrCodeService qrCodeService;


    /**
     * 로그인 회원이 보유한 모바일 티켓 목록 조회
     *
     * 좌석 1개 = 모바일 티켓 1장
     */
    public List<MyTicketResponse> findMemberTickets(Long memberId) {

        List<Reservation> reservations =
                reservationRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

        List<MyTicketResponse> tickets = new ArrayList<>();


        for (Reservation reservation : reservations) {

            /*
             * RESERVED 예매만 보유 티켓에 표시합니다.
             *
             * CANCELLED / PENDING / EXPIRED 예매는
             * 보유 티켓 화면에서 제외합니다.
             */
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

                /*
                 * 현재 티켓 상태 계산
                 *
                 * AVAILABLE : 이용 가능
                 * ENDED : 공연 종료
                 *
                 * TRANSFERRED는 TicketTransfer 구현 후
                 * 이 메서드에 추가합니다.
                 */
                String ticketStatus =
                        determineTicketStatus(
                                schedule.getStartsAt(),
                                performance.getRuntimeMinutes());


                /*
                 * 좌석별 QR 생성
                 */
                String qrContent =
                        "STAGEON:TICKET:" + seat.getId();

                String qrCodeImage =
                        qrCodeService.generateQrCode(qrContent);


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

                                qrCodeImage
                        );

                tickets.add(ticket);
            }
        }

        return tickets;
    }


    /**
     * 모바일 티켓의 현재 상태를 판단합니다.
     *
     * 공연 시작 시간 + 러닝타임을 기준으로
     * 공연 종료 여부를 계산합니다.
     *
     * AVAILABLE : 이용 가능
     * ENDED     : 공연 종료
     *
     * TRANSFERRED는 티켓 전달 기능 구현 후 추가합니다.
     */
    private String determineTicketStatus(
            LocalDateTime startsAt,
            Integer runtimeMinutes
    ) {

        // 공연 시작 시간이 없으면 우선 이용 가능 처리
        if (startsAt == null) {
            return "AVAILABLE";
        }

        // 러닝타임 정보가 없거나 잘못된 값이면
        // 임의로 종료 처리하지 않습니다.
        if (runtimeMinutes == null || runtimeMinutes <= 0) {
            return "AVAILABLE";
        }

        // 공연 종료 시각 = 공연 시작 시각 + 러닝타임(분)
        LocalDateTime endTime =
                startsAt.plusMinutes(runtimeMinutes);

        // 현재 시간이 공연 종료 시각을 지났으면 공연 종료
        if (LocalDateTime.now().isAfter(endTime)) {
            return "ENDED";
        }

        return "AVAILABLE";
    }
}