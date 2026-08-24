package kr.co.stageon.ai.web;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import kr.co.stageon.ai.dto.AiChatRequest;
import kr.co.stageon.ai.dto.AiGatewayChatRequest;
import kr.co.stageon.ai.dto.AiConversationSnapshot;
import kr.co.stageon.ai.service.AiConversationMemoryService;
import kr.co.stageon.ai.service.AiQuestionIntentService;
import kr.co.stageon.ai.service.StageonBookablePerformanceService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@RestController
@RequestMapping("/api/ai")
public class AiChatApiController {

    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;
    private final AiQuestionIntentService intentService;
    private final StageonBookablePerformanceService stageonPerformanceService;
    private final AiConversationMemoryService memoryService;
    private final String memoryMode;

    public AiChatApiController(
            @Qualifier("stageonAiRestClient") RestClient aiRestClient,
            ObjectMapper objectMapper,
            AiQuestionIntentService intentService,
            StageonBookablePerformanceService stageonPerformanceService,
            AiConversationMemoryService memoryService,
            @Value("${stageon.ai.memory-mode}") String memoryMode
    ) {
        this.aiRestClient = aiRestClient;
        this.objectMapper = objectMapper;
        this.intentService = intentService;
        this.stageonPerformanceService = stageonPerformanceService;
        this.memoryService = memoryService;
        this.memoryMode = memoryMode;
    }

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> chat(@Valid @RequestBody AiChatRequest request) {
        try {
            AiGatewayChatRequest gatewayRequest = createGatewayRequest(request);
            ResponseEntity<JsonNode> response = aiRestClient.post()
                    .uri("/api/v1/performance-chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(gatewayRequest)
                    .retrieve()
                    .toEntity(JsonNode.class);
            memoryService.remember(request, response.getBody());
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

    private AiGatewayChatRequest createGatewayRequest(AiChatRequest request) {
        AiConversationSnapshot snapshot = memoryService.load(request.conversationId());
        boolean hasPreviousPerformances = snapshot.hasConversation()
                && snapshot.lastPerformances() != null
                && !snapshot.lastPerformances().isEmpty();
        if (intentService.shouldReusePreviousResults(
                request.message(), hasPreviousPerformances, memoryMode)) {
            return AiGatewayChatRequest.followUp(request, snapshot);
        }
        if (!intentService.requiresStageonBookableData(request.message())) {
            return AiGatewayChatRequest.kopis(request, snapshot.messages());
        }
        return AiGatewayChatRequest.stageon(
                request,
                stageonPerformanceService.search(request.message(), 5),
                snapshot.messages()
        );
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
