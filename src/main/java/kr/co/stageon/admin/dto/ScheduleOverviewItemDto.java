package kr.co.stageon.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 상단 "회차 운영 현황" 요약 테이블(및 충돌 모달)의 한 행입니다. */
@Getter
@AllArgsConstructor
public class ScheduleOverviewItemDto {
    private final Long id;
    private final String performanceTitle;
    private final String dateTimeText;
    private final String statusText;
    private final String badgeClass;
    /** 같은 공연장 홀에서 시간대가 겹치는 회차인지 여부입니다. */
    private final boolean conflict;

    /** "관리 > 수정" 모달을 채우기 위한 원본 값들입니다. 비어있을 수 있는 값은 null 허용. */
    private final Long performanceId;
    private final Long venueHallId;
    private final Integer roundNumber;
    private final Integer maxTicketsPerMember;
    private final String startsAtIso;
    private final String salesOpenAtIso;
    private final String salesCloseAtIso;
    private final String cancelCloseAtIso;
}