package kr.co.stageon.ai.dto;

import java.util.List;

public record AiConversationSnapshot(
        List<AiConversationMessage> messages,
        String lastDataSource,
        List<AiPerformanceContext> lastPerformances
) {
    public static AiConversationSnapshot empty() {
        return new AiConversationSnapshot(List.of(), null, List.of());
    }

    public boolean hasConversation() {
        return messages != null && !messages.isEmpty();
    }
}
