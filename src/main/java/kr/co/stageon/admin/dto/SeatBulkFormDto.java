package kr.co.stageon.admin.dto;

import lombok.Getter;
import lombok.Setter;

/** 구역 단위로 좌석을 한 번에 여러 개 생성하기 위한 폼 DTO입니다. */
@Getter
@Setter
public class SeatBulkFormDto {
    private Long seatGradeId;
    private String sectionName;
    private Integer startRowNumber;
    private Integer rowCount;
    private Integer startSeatNumber;
    private Integer seatsPerRow;
}