package kr.co.stageon.payment.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@RequiredArgsConstructor
public enum CancelFeePolicy {
    // 예시: 8일 전 전액환불, 7~3일 전 10% 공제, 2~1일 전 20% 공제, 당일 환불 불가(100% 공제)
    FREE(8, 0.0),
    D_7_TO_3(7, 0.10),
    D_2_TO_1(2, 0.20),
    D_DAY(0, 1.00);

    private final int daysBefore;
    private final double feeRate;

    public static CancelFeePolicy getPolicy(LocalDateTime now, LocalDateTime performanceStartAt) {
        long daysBetween = ChronoUnit.DAYS.between(now.toLocalDate(), performanceStartAt.toLocalDate());

        if (daysBetween >= 8) return FREE;
        if (daysBetween >= 3) return D_7_TO_3;
        if (daysBetween >= 1) return D_2_TO_1;
        return D_DAY;
    }

    public BigDecimal calculateRefundAmount(BigDecimal totalAmount) {
        BigDecimal fee = totalAmount.multiply(BigDecimal.valueOf(this.feeRate));
        return totalAmount.subtract(fee).setScale(0, RoundingMode.DOWN); // 소수점 버림
    }
}