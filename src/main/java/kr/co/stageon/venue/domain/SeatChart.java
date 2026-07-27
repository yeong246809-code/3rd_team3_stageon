package kr.co.stageon.venue.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 홀에서 사용하는 좌석도 버전과 Seats.io 차트 식별자를 관리합니다. */
@Getter
@Entity
@Table(
        name = "seat_charts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_seat_chart_version",
                columnNames = {"venue_hall_id", "version"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatChart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_hall_id", nullable = false)
    private VenueHall venueHall;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "seatsio_chart_key", unique = true, length = 100)
    private String seatsioChartKey;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
