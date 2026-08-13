package kr.co.stageon.performance.support;

import kr.co.stageon.performance.domain.PerformanceSchedule;

import java.time.LocalDateTime;

/**
 * 공연 회차의 실제 예매 가능 여부를 한 곳에서 판단합니다.
 *
 * DB의 OPEN / SCHEDULED / CLOSED 문자열을 예매 가능 여부의 기준으로 사용하지 않고,
 * salesOpenAt / salesCloseAt과 현재 시간을 비교해 자동 오픈·자동 마감합니다.
 *
 * CANCELLED는 관리자가 강제로 취소한 상태이므로 항상 예매 불가입니다.
 */
public final class ScheduleSalesPolicy {

    private ScheduleSalesPolicy() {
    }

    /**
     * 현재 시각을 기준으로 해당 회차를 예매할 수 있는지 확인합니다.
     *
     * 조건: salesOpenAt <= now < salesCloseAt
     */
    public static boolean isBookable(
            PerformanceSchedule schedule,
            LocalDateTime now
    ) {
        if (schedule == null || now == null) {
            return false;
        }

        // 관리자가 공연을 취소한 경우에는 시간과 관계없이 예매할 수 없습니다.
        if (schedule.getStatus() == PerformanceSchedule.Status.CANCELLED) {
            return false;
        }

        LocalDateTime salesOpenAt = schedule.getSalesOpenAt();
        LocalDateTime salesCloseAt = schedule.getSalesCloseAt();

        if (salesOpenAt == null || salesCloseAt == null) {
            return false;
        }

        // salesOpenAt <= now < salesCloseAt
        return !now.isBefore(salesOpenAt)
                && now.isBefore(salesCloseAt);
    }

    /** 현재 시각 기준 편의 메서드입니다. */
    public static boolean isBookableNow(PerformanceSchedule schedule) {
        return isBookable(schedule, LocalDateTime.now());
    }

    /** 아직 티켓 오픈 전인지 확인합니다. */
    public static boolean isBeforeOpen(
            PerformanceSchedule schedule,
            LocalDateTime now
    ) {
        return schedule != null
                && schedule.getSalesOpenAt() != null
                && now.isBefore(schedule.getSalesOpenAt());
    }

    /** 예매 마감 시간이 지났는지 확인합니다. */
    public static boolean isSalesClosed(
            PerformanceSchedule schedule,
            LocalDateTime now
    ) {
        return schedule != null
                && schedule.getSalesCloseAt() != null
                && !now.isBefore(schedule.getSalesCloseAt());
    }
}
