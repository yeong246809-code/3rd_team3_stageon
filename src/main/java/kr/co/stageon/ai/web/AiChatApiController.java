package kr.co.stageon.ai.web;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import kr.co.stageon.ai.dto.AiChatRequest;
import kr.co.stageon.ai.dto.AiGatewayChatRequest;
import kr.co.stageon.ai.dto.AiConversationSnapshot;
import kr.co.stageon.ai.dto.AiMemberContext;
import kr.co.stageon.ai.dto.AiPerformanceContext;
import kr.co.stageon.ai.service.AiChatHistoryService;
import kr.co.stageon.ai.service.AiConversationMemoryService;
import kr.co.stageon.ai.service.AiMemberContextService;
import kr.co.stageon.ai.service.AiQuestionIntentService;
import kr.co.stageon.ai.service.StageonBookablePerformanceService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import kr.co.stageon.member.domain.Member;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiChatApiController {
    private static final Logger log = LoggerFactory.getLogger(AiChatApiController.class);

    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;
    private final AiQuestionIntentService intentService;
    private final StageonBookablePerformanceService stageonPerformanceService;
    private final AiConversationMemoryService memoryService;
    private final AiMemberContextService memberContextService;
    private final AiChatHistoryService historyService;
    private final String memoryMode;

    public AiChatApiController(
            @Qualifier("stageonAiRestClient") RestClient aiRestClient,
            ObjectMapper objectMapper,
            AiQuestionIntentService intentService,
            StageonBookablePerformanceService stageonPerformanceService,
            AiConversationMemoryService memoryService,
            AiMemberContextService memberContextService,
            AiChatHistoryService historyService,
            @Value("${stageon.ai.memory-mode}") String memoryMode
    ) {
        this.aiRestClient = aiRestClient;
        this.objectMapper = objectMapper;
        this.intentService = intentService;
        this.stageonPerformanceService = stageonPerformanceService;
        this.memoryService = memoryService;
        this.memberContextService = memberContextService;
        this.historyService = historyService;
        this.memoryMode = memoryMode;
    }

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> chat(
            @Valid @RequestBody AiChatRequest request,
            Authentication authentication
    ) {
        try {
            Member member = memberContextService.currentMember(authentication).orElse(null);
            AiChatRequest effectiveRequest = member == null
                    ? request
                    : new AiChatRequest(
                            request.message(),
                            "member-" + member.getId() + ":" + safeConversationId(request.conversationId()));
            AiMemberContext memberContext = member == null
                    ? AiMemberContext.anonymous()
                    : memberContextService.build(member);
            AiGatewayChatRequest gatewayRequest = createGatewayRequest(
                    effectiveRequest, memberContext, member, request.conversationId());
            ResponseEntity<JsonNode> response = aiRestClient.post()
                    .uri("/api/v1/performance-chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(gatewayRequest)
                    .retrieve()
                    .toEntity(JsonNode.class);
            memoryService.remember(effectiveRequest, response.getBody());
            JsonNode body = response.getBody();
            if (member != null && body != null && body.path("allowed").asBoolean()) {
                try {
                    historyService.remember(
                            member, request.conversationId(), request.message(), body.path("answer").asText());
                } catch (RuntimeException exception) {
                    // 대화 저장소에 일시적인 문제가 있어도 이미 생성된 AI 답변은 사용자에게 전달합니다.
                    log.warn("AI chat history could not be saved for member {}", member.getId(), exception);
                }
            }
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (RestClientResponseException exception) {
            int downstreamStatus = exception.getStatusCode().value();
            if (downstreamStatus == 400 || downstreamStatus == 429) {
                return ResponseEntity.status(downstreamStatus).body(readErrorBody(exception));
            }
            return error(HttpStatus.BAD_GATEWAY, "AI 서버 응답을 처리하지 못했습니다.");
        } catch (ResourceAccessException exception) {
            return error(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
        } catch (Exception exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "AI 요청 처리 중 오류가 발생했습니다.");
        }
    }

    private AiGatewayChatRequest createGatewayRequest(
            AiChatRequest request,
            AiMemberContext memberContext,
            Member member,
            String clientConversationId
    ) {
        AiConversationSnapshot snapshot = memoryService.load(request.conversationId());
        List<kr.co.stageon.ai.dto.AiConversationMessage> history = snapshot.hasConversation()
                ? snapshot.messages()
                : historyService.conversationMessages(member, clientConversationId);
        boolean hasPreviousPerformances = snapshot.hasConversation()
                && "STAGEON".equalsIgnoreCase(snapshot.lastDataSource())
                && snapshot.lastPerformances() != null
                && !snapshot.lastPerformances().isEmpty();
        if (intentService.shouldReusePreviousResults(
                request.message(), hasPreviousPerformances, memoryMode)) {
            return AiGatewayChatRequest.followUp(request, snapshot, memberContext);
        }
        List<AiPerformanceContext> candidates = stageonPerformanceService.search(request.message(), 20);
        return AiGatewayChatRequest.stageon(
                request,
                memberContextService.personalize(candidates, memberContext, 5),
                history,
                memberContext
        );
    }

    private String safeConversationId(String conversationId) {
        return conversationId == null || conversationId.isBlank() ? "saved-conversation" : conversationId.trim();
    }

    private JsonNode readErrorBody(RestClientResponseException exception) {
        try {
            return objectMapper.readTree(exception.getResponseBodyAsString());
        } catch (Exception ignored) {
            return objectMapper.createObjectNode().put("error", "AI 요청을 처리하지 못했습니다.");
        }
    }

    private ResponseEntity<JsonNode> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(objectMapper.createObjectNode().put("error", message));
    }
}
