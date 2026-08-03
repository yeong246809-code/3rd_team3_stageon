package kr.co.stageon.venue.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 좌석도별 VIP/R/S 등의 기본 등급과 화면 표시 색상을 관리합니다. */
@Getter
@Entity
@Table(name = "seat_grades")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_chart_id", nullable = false)
    private SeatChart seatChart;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "display_color", nullable = false, length = 7)
    private String displayColor;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public static SeatGrade create(SeatChart seatChart, String name, String displayColor, Integer sortOrder) {
        SeatGrade g = new SeatGrade();
        g.seatChart = seatChart;
        g.name = name;
        g.displayColor = displayColor;
        g.sortOrder = sortOrder;
        return g;
    }
}