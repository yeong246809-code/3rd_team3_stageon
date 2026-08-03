package kr.co.stageon.admin.dto;

import lombok.Getter;
import lombok.Setter;

/** 홀 등록 폼 바인딩용 DTO입니다. */
@Getter
@Setter
public class VenueHallFormDto {
    private String name;
    private Integer seatCapacity;
    private Integer accessibleSeatCount;
    private String kopisHallId;
}