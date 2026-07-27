package kr.co.stageon.booking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 하나의 좌석 선점에 포함된 회차 좌석입니다. */
@Getter
@Entity
@Table(
        name = "seat_hold_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_seat_hold_item",
                columnNames = {"seat_hold_id", "schedule_seat_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatHoldItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_hold_id", nullable = false)
    private SeatHold seatHold;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_seat_id", nullable = false)
    private ScheduleSeat scheduleSeat;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
