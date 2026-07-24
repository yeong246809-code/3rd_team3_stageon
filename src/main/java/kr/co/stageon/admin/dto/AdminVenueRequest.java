package kr.co.stageon.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 관리자 공연장 등록·수정 화면의 입력 DTO입니다. */
public record AdminVenueRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 300) String address,
        @NotBlank @Size(max = 50) String region,
        @PositiveOrZero int totalSeatCount
) {
}
