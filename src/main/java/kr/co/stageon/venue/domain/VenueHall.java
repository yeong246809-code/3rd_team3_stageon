package kr.co.stageon.venue.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 공연시설 내부에서 실제 공연이 열리는 개별 홀입니다. */
@Getter
@Entity
@Table(
        name = "venue_halls",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_venue_hall_name",
                columnNames = {"venue_id", "name"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenueHall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(name = "kopis_hall_id", unique = true, length = 50)
    private String kopisHallId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "seat_capacity", nullable = false)
    private Integer seatCapacity;

    @Column(name = "accessible_seat_count")
    private Integer accessibleSeatCount;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
