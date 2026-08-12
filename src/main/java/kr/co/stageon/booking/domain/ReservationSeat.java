package kr.co.stageon.booking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 예매 시점의 좌석과 가격을 보존하는 연결 엔티티입니다. */
@Getter
@Entity
@Table(
        name = "reservation_seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservation_schedule_seat",
                columnNames = {"reservation_id", "schedule_seat_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationSeat {

    public enum Status { RESERVED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_seat_id", nullable = false)
    private ScheduleSeat scheduleSeat;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "captured_section_name", nullable = false, length = 50)
    private String capturedSectionName;

    @Column(name = "captured_row_label", nullable = false, length = 20)
    private String capturedRowLabel;

    @Column(name = "captured_seat_number", nullable = false, length = 20)
    private String capturedSeatNumber;

    @Column(name = "captured_grade_name", nullable = false, length = 30)
    private String capturedGradeName;

    @Column(name = "captured_unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal capturedUnitPrice;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ReservationSeat create(Reservation reservation, ScheduleSeat scheduleSeat) {
        ReservationSeat rs = new ReservationSeat();
        rs.reservation = reservation;
        rs.scheduleSeat = scheduleSeat;

        // 생성 시 기본 상태를 RESERVED로 설정
        rs.status = Status.RESERVED;

        rs.capturedSectionName = scheduleSeat.getSeat().getSectionName();
        rs.capturedRowLabel = scheduleSeat.getSeat().getRowLabel();
        rs.capturedSeatNumber = scheduleSeat.getSeat().getSeatNumber();
        rs.capturedGradeName = scheduleSeat.getSeat().getSeatGrade().getName();

        // 결제 당시의 단가 스냅샷
        rs.capturedUnitPrice = scheduleSeat.getPrice();

        return rs;
    }

    public void cancel() {
        this.status = Status.CANCELLED;
    }
}