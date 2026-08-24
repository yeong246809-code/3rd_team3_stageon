package kr.co.stageon.ai.dto;

import java.time.LocalDate;
import java.util.List;

public record AiGatewayChatRequest(
        String message,
        String conversationId,
        String dataSource,
        LocalDate dataUpdatedAt,
        List<AiPerformanceContext> performances,
        List<AiConversationMessage> conversationHistory,
        boolean reusePreviousResults
) {
    public static AiGatewayChatRequest kopis(
            AiChatRequest request,
            List<AiConversationMessage> history
    ) {
        return new AiGatewayChatRequest(
                request.message(), request.conversationId(), "KOPIS", null,
                List.of(), List.copyOf(history), false
        );
    }

    public static AiGatewayChatRequest stageon(
            AiChatRequest request,
            List<AiPerformanceContext> performances,
            List<AiConversationMessage> history
    ) {
        return new AiGatewayChatRequest(
                request.message(), request.conversationId(), "STAGEON",
                LocalDate.now(), List.copyOf(performances), List.copyOf(history), false
        );
    }

    public static AiGatewayChatRequest followUp(
            AiChatRequest request,
            AiConversationSnapshot snapshot
    ) {
        return new AiGatewayChatRequest(
                request.message(), request.conversationId(), snapshot.lastDataSource(),
                LocalDate.now(), List.copyOf(snapshot.lastPerformances()),
                List.copyOf(snapshot.messages()), true
        );
    }
}
