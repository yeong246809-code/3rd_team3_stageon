package kr.co.stageon.admin.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** "회차 수정" 모달에서 전달되는 입력값입니다. 공연·홀은 변경하지 않습니다. */
@Getter
@Setter
public class ScheduleEditFormDto {
    private Integer roundNumber;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startsAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime salesOpenAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime salesCloseAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime cancelCloseAt;

    private Integer maxTicketsPerMember;
}