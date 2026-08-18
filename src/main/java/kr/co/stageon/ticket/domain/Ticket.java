package kr.co.stageon.ticket.domain; // 패키지 경로는 프로젝트에 맞게 수정하세요

import jakarta.persistence.*;
import kr.co.stageon.booking.domain.ReservationSeat;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 예약 좌석과 1:1 매핑 (유니크 키)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_seat_id", nullable = false, unique = true)
    private ReservationSeat reservationSeat;

    @Column(name = "ticket_number", nullable = false, length = 30)
    private String ticketNumber; // 예약의 bookingNumber와 동일하게 맞춤

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "qr_token_hash", length = 64, unique = true)
    private String qrTokenHash;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        ISSUED, USED, CANCELLED
    }

    @Builder
    public Ticket(ReservationSeat reservationSeat, String ticketNumber, String qrTokenHash) {
        this.reservationSeat = reservationSeat;
        this.ticketNumber = ticketNumber;
        this.status = Status.ISSUED;
        this.qrTokenHash = qrTokenHash;
        this.issuedAt = LocalDateTime.now();
    }

    // 취소 시 상태 변경 메서드
    public void cancel() {
        this.status = Status.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }
}