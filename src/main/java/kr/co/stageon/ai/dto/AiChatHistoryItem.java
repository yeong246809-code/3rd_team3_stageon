package kr.co.stageon.ai.dto;

import java.time.LocalDateTime;

public record AiChatHistoryItem(
        Long id,
        String question,
        String answer,
        LocalDateTime createdAt
) {
}
