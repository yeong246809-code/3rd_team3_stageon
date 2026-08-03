package kr.co.stageon.admin.dto;

import lombok.Getter;
import lombok.Setter;

/** 좌석 등급(예: VIP/R/S) 1개 등록 폼 바인딩용 DTO입니다. 등급은 한 번에 하나씩 추가합니다. */
@Getter
@Setter
public class SeatGradeFormDto {
    private String name;
    private String displayColor;
    private Integer sortOrder;
}