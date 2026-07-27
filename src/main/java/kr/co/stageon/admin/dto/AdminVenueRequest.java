package kr.co.stageon.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 관리자 공연장 등록·수정 화면의 입력 DTO입니다. */
public record AdminVenueRequest(
        @Size(max = 50) String kopisFacilityId,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 300) String address,
        @NotBlank @Size(max = 50) String region,
        BigDecimal latitude,
        BigDecimal longitude,
        @Size(max = 30) String phone,
        @Size(max = 500) String homepageUrl
) {
}
