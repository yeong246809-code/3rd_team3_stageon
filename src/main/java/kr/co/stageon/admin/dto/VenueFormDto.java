package kr.co.stageon.admin.dto;

import kr.co.stageon.venue.domain.Venue;
import lombok.Getter;
import lombok.Setter;

/** 공연장(시설) 등록·수정 겸용 폼 바인딩용 DTO입니다. */
@Getter
@Setter
public class VenueFormDto {
    private Long id;
    private String kopisFacilityId;
    private String name;
    private String address;
    private String region;
    private String phone;
    private String homepageUrl;

    public static VenueFormDto from(Venue v) {
        VenueFormDto dto = new VenueFormDto();
        dto.id = v.getId();
        dto.kopisFacilityId = v.getKopisFacilityId();
        dto.name = v.getName();
        dto.address = v.getAddress();
        dto.region = v.getRegion();
        dto.phone = v.getPhone();
        dto.homepageUrl = v.getHomepageUrl();
        return dto;
    }
}