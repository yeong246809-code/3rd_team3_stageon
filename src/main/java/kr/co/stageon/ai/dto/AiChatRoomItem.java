package kr.co.stageon.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AiChatRoomItem(
        String conversationId,
        String title,
        LocalDateTime updatedAt,
        List<AiChatHistoryItem> messages
) {
}
