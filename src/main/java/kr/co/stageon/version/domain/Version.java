package kr.co.stageon.version.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 프로젝트 버전별 변경 이력(체인지로그)입니다. */
@Entity
@Table(name = "versions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Version {

    public enum Category {
        FEATURE, IMPROVEMENT, BUGFIX
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version", length = 30, nullable = false)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private Category category;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "author", length = 80, nullable = false)
    private String author;

    @Column(name = "released_at", nullable = false)
    private LocalDate releasedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Version(String version, Category category, String title, String description,
                    String author, LocalDate releasedAt) {
        this.version = version;
        this.category = category;
        this.title = title;
        this.description = description;
        this.author = author;
        this.releasedAt = releasedAt;
    }

    public static Version create(String version, Category category, String title, String description,
                                 String author, LocalDate releasedAt) {
        return new Version(version, category, title, description, author, releasedAt);
    }

    public void update(String version, Category category, String title, String description,
                       String author, LocalDate releasedAt) {
        this.version = version;
        this.category = category;
        this.title = title;
        this.description = description;
        this.author = author;
        this.releasedAt = releasedAt;
    }
}