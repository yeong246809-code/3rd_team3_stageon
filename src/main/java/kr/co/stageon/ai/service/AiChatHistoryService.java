package kr.co.stageon.ai.service;

import kr.co.stageon.ai.domain.AiChatHistory;
import kr.co.stageon.ai.dto.AiChatHistoryItem;
import kr.co.stageon.ai.dto.AiChatRoomItem;
import kr.co.stageon.ai.dto.AiConversationMessage;
import kr.co.stageon.ai.repository.AiChatHistoryRepository;
import kr.co.stageon.member.domain.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AiChatHistoryService {
    public static final int ROOM_LIMIT = 10;
    private static final int MESSAGE_LIMIT_PER_ROOM = 20;
    private static final String LEGACY_ROOM_ID = "saved-conversation";

    private final AiChatHistoryRepository repository;
    private final ObjectMapper objectMapper;

    public AiChatHistoryService(AiChatHistoryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void remember(Member member, String conversationId, String question, String answer) {
        if (member == null || answer == null || answer.isBlank()) return;
        String roomId = safeRoomId(conversationId);
        List<AiChatHistory> currentRows = repository.findByMemberIdOrderByCreatedAtDesc(member.getId());
        String title = currentRows.stream()
                .filter(row -> roomId.equals(metadata(row).conversationId()))
                .map(row -> metadata(row).title())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElseGet(() -> titleFrom(question));
        repository.saveAndFlush(AiChatHistory.success(
                member,
                question,
                answer,
                objectMapper.writeValueAsString(new RoomMetadata(roomId, title))
        ));
        pruneOldRooms(member);
    }

    @Transactional(readOnly = true)
    public List<AiChatHistoryItem> recent(Member member) {
        List<AiChatRoomItem> rooms = rooms(member);
        return rooms.isEmpty() ? List.of() : rooms.getFirst().messages();
    }

    @Transactional(readOnly = true)
    public List<AiChatRoomItem> rooms(Member member) {
        if (member == null) return List.of();
        Map<String, RoomAccumulator> grouped = new LinkedHashMap<>();
        for (AiChatHistory row : repository.findByMemberIdOrderByCreatedAtDesc(member.getId())) {
            RoomMetadata metadata = metadata(row);
            if (!grouped.containsKey(metadata.conversationId()) && grouped.size() >= ROOM_LIMIT) continue;
            RoomAccumulator room = grouped.computeIfAbsent(metadata.conversationId(), ignored ->
                    new RoomAccumulator(metadata.title(), row.getCreatedAt(), new ArrayList<>()));
            if (room.messages().size() < MESSAGE_LIMIT_PER_ROOM) {
                room.messages().add(toItem(row));
            }
        }
        return grouped.entrySet().stream().map(entry -> {
            List<AiChatHistoryItem> messages = new ArrayList<>(entry.getValue().messages());
            Collections.reverse(messages);
            return new AiChatRoomItem(
                    entry.getKey(), entry.getValue().title(), entry.getValue().updatedAt(), List.copyOf(messages));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<AiConversationMessage> conversationMessages(Member member, String conversationId) {
        String roomId = safeRoomId(conversationId);
        return rooms(member).stream()
                .filter(room -> room.conversationId().equals(roomId))
                .findFirst()
                .map(room -> room.messages().stream()
                        .skip(Math.max(0, room.messages().size() - 5L))
                        .flatMap(item -> List.of(
                                new AiConversationMessage("user", item.question()),
                                new AiConversationMessage("assistant", item.answer())
                        ).stream())
                        .toList())
                .orElseGet(List::of);
    }

    @Transactional
    public void clear(Member member) {
        if (member != null) repository.deleteByMemberId(member.getId());
    }

    private void pruneOldRooms(Member member) {
        List<AiChatHistory> rows = repository.findByMemberIdOrderByCreatedAtDesc(member.getId());
        Set<String> keptRoomIds = new LinkedHashSet<>();
        Set<String> obsoleteRoomIds = new LinkedHashSet<>();
        for (AiChatHistory row : rows) {
            String roomId = metadata(row).conversationId();
            if (keptRoomIds.contains(roomId) || obsoleteRoomIds.contains(roomId)) continue;
            if (keptRoomIds.size() < ROOM_LIMIT) keptRoomIds.add(roomId);
            else obsoleteRoomIds.add(roomId);
        }
        List<AiChatHistory> obsolete = rows.stream()
                .filter(row -> obsoleteRoomIds.contains(metadata(row).conversationId()))
                .toList();
        if (!obsolete.isEmpty()) repository.deleteAllInBatch(obsolete);
    }

    private AiChatHistoryItem toItem(AiChatHistory row) {
        return new AiChatHistoryItem(row.getId(), row.getQuestion(), row.getAnswer(), row.getCreatedAt());
    }

    private RoomMetadata metadata(AiChatHistory row) {
        String json = row.getExtractedCondition();
        if (json == null || json.isBlank()) return new RoomMetadata(LEGACY_ROOM_ID, "이전 AI 대화");
        try {
            RoomMetadata metadata = objectMapper.readValue(json, RoomMetadata.class);
            return new RoomMetadata(
                    safeRoomId(metadata.conversationId()),
                    metadata.title() == null || metadata.title().isBlank() ? "공연 추천 대화" : metadata.title());
        } catch (Exception ignored) {
            return new RoomMetadata(LEGACY_ROOM_ID, "이전 AI 대화");
        }
    }

    private String safeRoomId(String value) {
        if (value == null || value.isBlank()) return LEGACY_ROOM_ID;
        String trimmed = value.trim();
        return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
    }

    private String titleFrom(String question) {
        if (question == null || question.isBlank()) return "새 공연 추천 대화";
        String normalized = question.trim().replaceAll("\\s+", " ");
        return normalized.length() > 28 ? normalized.substring(0, 28) + "…" : normalized;
    }

    private record RoomMetadata(String conversationId, String title) {
    }

    private record RoomAccumulator(
            String title,
            java.time.LocalDateTime updatedAt,
            List<AiChatHistoryItem> messages
    ) {
    }
}
