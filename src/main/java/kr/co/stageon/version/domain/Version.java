package kr.co.stageon.version.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 프로젝트 버전별 배포 이력입니다. 관리자가 수동으로 등록하며, 사이트 공개 푸터에 노출됩니다. */
@Entity
@Table(name = "versions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Version {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version", length = 30, nullable = false)
    private String version;

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

    private Version(String version, String description, String author, LocalDate releasedAt) {
        this.version = version;
        this.description = description;
        this.author = author;
        this.releasedAt = releasedAt;
    }

    public static Version create(String version, String description, String author, LocalDate releasedAt) {
        return new Version(version, description, author, releasedAt);
    }

    public void update(String version, String description, String author, LocalDate releasedAt) {
        this.version = version;
        this.description = description;
        this.author = author;
        this.releasedAt = releasedAt;
    }
}