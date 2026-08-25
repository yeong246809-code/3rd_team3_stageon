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
        AiMemberContext memberContext,
        boolean reusePreviousResults
) {
    public static AiGatewayChatRequest stageon(
            AiChatRequest request,
            List<AiPerformanceContext> performances,
            List<AiConversationMessage> history,
            AiMemberContext memberContext
    ) {
        return new AiGatewayChatRequest(
                request.message(), request.conversationId(), "STAGEON",
                LocalDate.now(), List.copyOf(performances), List.copyOf(history), memberContext, false
        );
    }

    public static AiGatewayChatRequest followUp(
            AiChatRequest request,
            AiConversationSnapshot snapshot,
            AiMemberContext memberContext
    ) {
        return new AiGatewayChatRequest(
                request.message(), request.conversationId(), "STAGEON",
                LocalDate.now(), List.copyOf(snapshot.lastPerformances()),
                List.copyOf(snapshot.messages()), memberContext, true
        );
    }
}
