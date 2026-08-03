package kr.co.stageon.admin.dto;

import lombok.Getter;
import lombok.Setter;

/** 공연장(시설) 추가 폼 바인딩용 DTO입니다. */
@Getter
@Setter
public class VenueFormDto {
    private String kopisFacilityId;
    private String name;
    private String address;
    private String region;
    private String phone;
    private String homepageUrl;
}