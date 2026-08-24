package kr.co.stageon.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        @NotBlank(message = "질문을 입력해 주세요.")
        @Size(max = 500, message = "질문은 500자 이하여야 합니다.")
        String message,

        @Size(max = 100, message = "대화 식별자가 너무 깁니다.")
        String conversationId
) {
}
