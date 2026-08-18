package kr.co.stageon.banner.domain;

import jakarta.persistence.*;
import kr.co.stageon.performance.domain.Performance;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 메인 페이지 히어로 슬라이더에 노출되는 관리자 등록 배너입니다. */
@Getter
@Entity
@Table(name = "banners")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id")
    private Performance performance;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "badge_text", length = 50)
    private String badgeText;

    @Column(name = "button1_text", nullable = false, length = 30)
    private String button1Text;

    @Column(name = "button1_url", length = 500)
    private String button1Url;

    @Column(name = "button2_text", nullable = false, length = 30)
    private String button2Text;

    @Column(name = "button2_url", length = 500)
    private String button2Url;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public static Banner create(String title, String description, String imageUrl, Performance performance,
                                String linkUrl, LocalDate periodStart, LocalDate periodEnd, String badgeText,
                                String button1Text, String button1Url, String button2Text, String button2Url,
                                int displayOrder, boolean active) {
        Banner b = new Banner();
        b.title = title;
        b.description = description;
        b.imageUrl = imageUrl;
        b.performance = performance;
        b.linkUrl = linkUrl;
        b.periodStart = periodStart;
        b.periodEnd = periodEnd;
        b.badgeText = badgeText;
        b.button1Text = button1Text;
        b.button1Url = button1Url;
        b.button2Text = button2Text;
        b.button2Url = button2Url;
        b.displayOrder = displayOrder;
        b.active = active;
        return b;
    }

    public void update(String title, String description, String imageUrl, Performance performance,
                       String linkUrl, LocalDate periodStart, LocalDate periodEnd, String badgeText,
                       String button1Text, String button1Url, String button2Text, String button2Url,
                       boolean active) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.performance = performance;
        this.linkUrl = linkUrl;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.badgeText = badgeText;
        this.button1Text = button1Text;
        this.button1Url = button1Url;
        this.button2Text = button2Text;
        this.button2Url = button2Url;
        this.active = active;
    }

    public void changeOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void toggleActive() {
        this.active = !this.active;
    }
}