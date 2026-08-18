package kr.co.stageon.ticket.repository;

import kr.co.stageon.ticket.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByReservationSeatId(Long reservationSeatId);
}