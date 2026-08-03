package kr.co.stageon.venue.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 여러 공연장(홀)을 포함할 수 있는 공연시설입니다. */
@Getter
@Entity
@Table(name = "venues")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kopis_facility_id", unique = true, length = 50)
    private String kopisFacilityId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(nullable = false, length = 50)
    private String region;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 30)
    private String phone;

    @Column(name = "homepage_url", length = 500)
    private String homepageUrl;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public static Venue create(String kopisFacilityId, String name, String address, String region,
                               BigDecimal latitude, BigDecimal longitude, String phone, String homepageUrl) {
        Venue v = new Venue();
        v.kopisFacilityId = kopisFacilityId;
        v.name = name;
        v.address = address;
        v.region = region;
        v.latitude = latitude;
        v.longitude = longitude;
        v.phone = phone;
        v.homepageUrl = homepageUrl;
        return v;
    }

    public void update(String kopisFacilityId, String name, String address, String region,
                       BigDecimal latitude, BigDecimal longitude, String phone, String homepageUrl) {
        this.kopisFacilityId = kopisFacilityId;
        this.name = name;
        this.address = address;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.homepageUrl = homepageUrl;
    }
}