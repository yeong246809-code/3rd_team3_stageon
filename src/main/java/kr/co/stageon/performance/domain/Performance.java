package kr.co.stageon.performance.domain;

import jakarta.persistence.*;
import kr.co.stageon.venue.domain.VenueHall;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** KOPIS 또는 관리자가 등록한 공연 기본 정보입니다. */
@Getter
@Entity
@Table(name = "performances")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Performance {

    public enum Status { UPCOMING, ON_SALE, ENDED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kopis_id", unique = true, length = 50)
    private String kopisId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50)
    private String genre;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "runtime_minutes")
    private Integer runtimeMinutes;

    @Column(name = "age_text", length = 100)
    private String ageText;

    @Column(columnDefinition = "text")
    private String story;

    @Column(name = "raw_price_text", columnDefinition = "text")
    private String rawPriceText;

    @Column(name = "raw_schedule_text", columnDefinition = "text")
    private String rawScheduleText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    /** 임시저장 여부입니다. true면 목록에서 "임시저장됨"으로 표시됩니다. */
    @Column(name = "is_draft", nullable = false)
    private boolean draft;

    /** 공연장 중복 예약 방지에 사용되는 홀 연관관계입니다. 동일 hall+동일 기간에는 이 performanceId 외 등록을 막습니다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_hall_id")
    private VenueHall venueHall;

    /** 공연 기본 가격입니다(등급별 가격은 PerformanceSeatPrice에서 별도 관리). */
    @Column(name = "base_price")
    private Integer basePrice;

    @Column(name = "kopis_updated_at")
    private LocalDateTime kopisUpdatedAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /** 등급별 가격(JSON)을 raw_price_text 컬럼에 저장합니다. 새 테이블 없이 기존 컬럼을 재사용합니다. */
    public void updateSeatPriceJson(String json) {
        this.rawPriceText = json;
    }

    /** 관리자 등록 폼에서 신규 생성 시 사용합니다. */
    public static Performance create(String kopisId, String title, String genre, String posterUrl,
                                     LocalDate startDate, LocalDate endDate, Integer runtimeMinutes,
                                     String ageText, String story, Status status, boolean draft,
                                     VenueHall venueHall, Integer basePrice) {
        Performance p = new Performance();
        p.kopisId = kopisId;
        p.title = title;
        p.genre = genre;
        p.posterUrl = posterUrl;
        p.startDate = startDate;
        p.endDate = endDate;
        p.runtimeMinutes = runtimeMinutes;
        p.ageText = ageText;
        p.story = story;
        p.status = status;
        p.draft = draft;
        p.venueHall = venueHall;
        p.basePrice = basePrice;
        return p;
    }

    /** 관리자 수정 폼에서 필드를 갱신할 때 사용합니다. */
    public void update(String kopisId, String title, String genre, String posterUrl,
                       LocalDate startDate, LocalDate endDate, Integer runtimeMinutes,
                       String ageText, String story, Status status, boolean draft,
                       VenueHall venueHall, Integer basePrice) {
        this.kopisId = kopisId;
        this.title = title;
        this.genre = genre;
        this.posterUrl = posterUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.runtimeMinutes = runtimeMinutes;
        this.ageText = ageText;
        this.story = story;
        this.status = status;
        this.draft = draft;
        this.venueHall = venueHall;
        this.basePrice = basePrice;
    }

    /**
     * 공연 종료 일자가 지나면 상태를 ENDED로 변경합니다.
     */
    public void markAsEnded() {
        this.status = Status.ENDED;
    }
}