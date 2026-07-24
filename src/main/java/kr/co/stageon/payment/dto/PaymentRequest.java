package kr.co.stageon.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** 모의 결제 요청 DTO입니다. idempotencyKey는 요청 재시도에도 동일해야 합니다. */
public record PaymentRequest(
        @NotNull @Positive Long reservationId,
        @NotBlank String idempotencyKey,
        @NotNull @Positive BigDecimal amount,
        boolean simulateFailure
) {
}
