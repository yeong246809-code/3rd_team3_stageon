package kr.co.stageon.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 공연·월로 필터링한 하단 상세 회차 목록의 한 행입니다. */
@Getter
@AllArgsConstructor
public class ScheduleListItemDto {
    private final Long id;
    private final String dateText;
    private final String roundText;
    private final String timeText;
    private final String statusText;
    private final String badgeClass;
    private final long remainingSeats;
}