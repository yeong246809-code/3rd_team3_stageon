package kr.co.stageon.queue.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QueueInfoResponse {
    private Long performanceId;
    private Long scheduleId;
    private String status;              // WAITING, ENTERED, EXPIRED
    private Integer waitingOrder;       // 현재 내 앞 대기 인원
    private Integer estimatedMinutes;   // 예상 대기 시간(분)
    private Integer progressPercentage; // 진행률 (0~100%)

    // 상태가 WAITING이 아닐 때 사용하는 기본 응답 생성 메서드
    public static QueueInfoResponse ofNonWaiting(Long performanceId, Long scheduleId, String status) {
        return QueueInfoResponse.builder()
                .performanceId(performanceId)
                .scheduleId(scheduleId)
                .status(status)
                .waitingOrder(0)
                .estimatedMinutes(0)
                .progressPercentage(100)
                .build();
    }
}
