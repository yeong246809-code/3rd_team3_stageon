package kr.co.stageon.booking.dto;

import kr.co.stageon.booking.domain.Reservation.ReceiveMethod;

/** 결제 처리 및 예매 확정을 위해 화면에서 넘어오는 파라미터 DTO */
public record PaymentRequest(
        Long seatHoldId,
        String holdTokenHash,
        ReceiveMethod receiveMethod, // 이전 단계에서 만든 라디오 버튼 값
        String bookerName,           // 수정 가능한 이름
        String bookerEmail,          // 수정 가능한 이메일
        String bookerPhone           // 수정 가능한 연락처
) {
}