package kr.co.stageon.venue.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공연장의 물리 좌석입니다.
 * 회차별 판매 상태는 이 엔티티가 아니라 ScheduleSeat에서 관리합니다.
 */
@Getter
@Entity
@Table(
        name = "seats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_seat_chart_position",
                        columnNames = {"seat_chart_id", "section_name", "row_label", "seat_number"}
                ),
                @UniqueConstraint(
                        name = "uk_seat_chart_seatsio_object",
                        columnNames = {"seat_chart_id", "seatsio_object_key"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    public enum ObjectType { SEAT, TABLE, BOOTH, GENERAL_ADMISSION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_chart_id", nullable = false)
    private SeatChart seatChart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_grade_id", nullable = false)
    private SeatGrade seatGrade;

    @Column(name = "seatsio_object_key", length = 100)
    private String seatsioObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false, length = 30)
    private ObjectType objectType;

    @Column(name = "section_name", length = 50)
    private String sectionName;

    @Column(name = "row_label", length = 20)
    private String rowLabel;

    @Column(name = "seat_number", length = 20)
    private String seatNumber;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "accessible_seat", nullable = false)
    private boolean accessible;

    @Column(name = "blocked_default", nullable = false)
    private boolean blockedDefault;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
