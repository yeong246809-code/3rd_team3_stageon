package kr.co.stageon.ai.service;

import kr.co.stageon.ai.dto.AiChatRequest;
import kr.co.stageon.ai.dto.AiConversationMessage;
import kr.co.stageon.ai.dto.AiConversationSnapshot;
import kr.co.stageon.ai.dto.AiPerformanceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class AiConversationMemoryService {
    private static final Logger log = LoggerFactory.getLogger(AiConversationMemoryService.class);
    private static final String KEY_PREFIX = "stageon:ai:conversation:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final int maxTurns;

    public AiConversationMemoryService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            @Value("${stageon.ai.memory-ttl}") Duration ttl,
            @Value("${stageon.ai.memory-max-turns}") int maxTurns
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
        this.maxTurns = Math.max(1, Math.min(maxTurns, 10));
    }

    public AiConversationSnapshot load(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return AiConversationSnapshot.empty();
        }
        try {
            String json = redisTemplate.opsForValue().get(key(conversationId));
            if (json == null || json.isBlank()) {
                return AiConversationSnapshot.empty();
            }
            ConversationState state = objectMapper.readValue(json, ConversationState.class);
            if (state.turns() == null || state.turns().isEmpty()) {
                return AiConversationSnapshot.empty();
            }
            List<AiConversationMessage> messages = state.turns().stream()
                    .flatMap(turn -> List.of(
                            new AiConversationMessage("user", turn.question()),
                            new AiConversationMessage("assistant", turn.answer())
                    ).stream())
                    .toList();
            ConversationTurn lastTurn = state.turns().getLast();
            return new AiConversationSnapshot(
                    messages,
                    lastTurn.dataSource(),
                    safePerformances(lastTurn.performances())
            );
        } catch (Exception exception) {
            log.warn("AI 대화 기록을 Redis에서 읽지 못했습니다: {}", exception.getMessage());
            return AiConversationSnapshot.empty();
        }
    }

    public void remember(AiChatRequest request, JsonNode response) {
        if (request.conversationId() == null || request.conversationId().isBlank()
                || response == null || !response.path("allowed").asBoolean()) {
            return;
        }
        String answer = response.path("answer").asText().trim();
        if (answer.isBlank()) {
            return;
        }
        try {
            String redisKey = key(request.conversationId());
            ConversationState current = readState(redisKey);
            List<ConversationTurn> turns = new ArrayList<>(current.turns());
            turns.add(new ConversationTurn(
                    request.message(),
                    answer,
                    textOrDefault(response.path("dataSource"), "KOPIS"),
                    parseDate(response.path("dataUpdatedAt").asText()),
                    parsePerformances(response.path("performances")),
                    Instant.now()
            ));
            if (turns.size() > maxTurns) {
                turns = new ArrayList<>(turns.subList(turns.size() - maxTurns, turns.size()));
            }
            redisTemplate.opsForValue().set(
                    redisKey,
                    objectMapper.writeValueAsString(new ConversationState(List.copyOf(turns))),
                    ttl
            );
        } catch (Exception exception) {
            log.warn("AI 대화 기록을 Redis에 저장하지 못했습니다: {}", exception.getMessage());
        }
    }

    private ConversationState readState(String redisKey) {
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null || json.isBlank()) {
            return new ConversationState(List.of());
        }
        ConversationState state = objectMapper.readValue(json, ConversationState.class);
        return state.turns() == null ? new ConversationState(List.of()) : state;
    }

    private List<AiPerformanceContext> parsePerformances(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<AiPerformanceContext> performances = new ArrayList<>();
        node.forEach(item -> performances.add(objectMapper.treeToValue(item, AiPerformanceContext.class)));
        return List.copyOf(performances);
    }

    private List<AiPerformanceContext> safePerformances(List<AiPerformanceContext> performances) {
        return performances == null ? List.of() : List.copyOf(performances);
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        String value = node == null ? "" : node.asText();
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String key(String conversationId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(conversationId.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("대화 식별자를 처리하지 못했습니다.", exception);
        }
    }

    private record ConversationState(List<ConversationTurn> turns) {
    }

    private record ConversationTurn(
            String question,
            String answer,
            String dataSource,
            LocalDate dataUpdatedAt,
            List<AiPerformanceContext> performances,
            Instant createdAt
    ) {
    }
}
