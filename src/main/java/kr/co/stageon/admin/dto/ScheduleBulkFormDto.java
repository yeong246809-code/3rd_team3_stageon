package kr.co.stageon.admin.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * "회차 일괄 추가"(달력) 모달에서 전달되는 입력값입니다.
 * 달력에서 선택한 여러 날짜(datesCsv) x 하나의 시간(time) 조합으로 회차를 한 번에 생성합니다.
 */
@Getter
@Setter
public class ScheduleBulkFormDto {
    private Long performanceId;
    private Long venueHallId;

    /** 쉼표로 구분된 날짜 목록입니다. 예: "2026-08-08,2026-08-15,2026-08-22" */
    private String datesCsv;

    /** "HH:mm" 형식의 공연 시작 시간입니다. */
    private String time;

    private Integer maxTicketsPerMember;
}